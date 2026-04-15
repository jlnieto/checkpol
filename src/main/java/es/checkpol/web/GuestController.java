package es.checkpol.web;

import es.checkpol.domain.DocumentType;
import es.checkpol.domain.Guest;
import es.checkpol.domain.GuestReviewStatus;
import es.checkpol.domain.GuestRelationship;
import es.checkpol.service.AddressService;
import es.checkpol.service.BookingDetails;
import es.checkpol.service.BookingService;
import es.checkpol.service.GuestService;
import es.checkpol.service.GuestSelfServiceService;
import es.checkpol.service.SelfServiceAccess;
import es.checkpol.service.TravelerPartService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Controller
public class GuestController {

    private final AddressService addressService;
    private final BookingService bookingService;
    private final GuestService guestService;
    private final TravelerPartService travelerPartService;
    private final GuestSelfServiceService guestSelfServiceService;
    private final GuestWizardDraftStore guestWizardDraftStore;

    public GuestController(
        AddressService addressService,
        BookingService bookingService,
        GuestService guestService,
        TravelerPartService travelerPartService,
        GuestSelfServiceService guestSelfServiceService,
        GuestWizardDraftStore guestWizardDraftStore
    ) {
        this.addressService = addressService;
        this.bookingService = bookingService;
        this.guestService = guestService;
        this.travelerPartService = travelerPartService;
        this.guestSelfServiceService = guestSelfServiceService;
        this.guestWizardDraftStore = guestWizardDraftStore;
    }

    @GetMapping("/bookings/{bookingId}")
    public String details(@PathVariable Long bookingId, Model model) {
        model.addAttribute("details", bookingService.getDetails(bookingId));
        return "bookings/details";
    }

    @GetMapping("/bookings/{bookingId}/guests/new")
    public String newGuest(
        @PathVariable Long bookingId,
        @org.springframework.web.bind.annotation.RequestParam(name = "selectedAddressId", required = false) Long selectedAddressId,
        @org.springframework.web.bind.annotation.RequestParam(name = "step", required = false) Integer step,
        HttpSession session,
        Model model
    ) {
        boolean resumingDraft = step != null || selectedAddressId != null;
        if (!resumingDraft) {
            guestWizardDraftStore.clearBookingDraft(session, bookingId, null);
        }

        GuestForm form = guestWizardDraftStore.getBookingDraft(session, bookingId, null)
            .filter(draft -> resumingDraft)
            .map(draft -> selectedAddressId == null ? draft : draft.withAddressId(selectedAddressId))
            .orElseGet(() -> selectedAddressId == null ? new GuestForm() : new GuestForm().withAddressId(selectedAddressId));
        return populateForm(model, bookingId, form, "/bookings/" + bookingId + "/guests", "Nuevo huésped", "Guardar", null, step);
    }

    @PostMapping("/bookings/{bookingId}/guests")
    public String createGuest(
        @PathVariable Long bookingId,
        @Valid @ModelAttribute("guestForm") GuestForm form,
        BindingResult bindingResult,
        HttpSession session,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateForm(model, bookingId, form, "/bookings/" + bookingId + "/guests", "Nuevo huésped", "Guardar", null, 3);
        }

        try {
            guestService.create(bookingId, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("guest.invalid", exception.getMessage());
            return populateForm(model, bookingId, form, "/bookings/" + bookingId + "/guests", "Nuevo huésped", "Guardar", null, 3);
        }

        guestWizardDraftStore.clearBookingDraft(session, bookingId, null);
        redirectAttributes.addFlashAttribute("flashMessage", "Datos del huésped guardados.");
        redirectAttributes.addFlashAttribute("flashKind", "success");
        return "redirect:/bookings/" + bookingId;
    }

    @PostMapping("/bookings/{bookingId}/guests/draft-address")
    public String saveNewGuestDraftBeforeAddress(
        @PathVariable Long bookingId,
        @ModelAttribute("guestForm") GuestForm form,
        HttpSession session
    ) {
        guestWizardDraftStore.saveBookingDraft(session, bookingId, null, form);
        return "redirect:/bookings/" + bookingId + "/addresses/new";
    }

    @GetMapping("/bookings/{bookingId}/guests/{guestId}/edit")
    public String editGuest(
        @PathVariable Long bookingId,
        @PathVariable Long guestId,
        @org.springframework.web.bind.annotation.RequestParam(name = "selectedAddressId", required = false) Long selectedAddressId,
        @org.springframework.web.bind.annotation.RequestParam(name = "step", required = false) Integer step,
        HttpSession session,
        Model model
    ) {
        boolean resumingDraft = step != null || selectedAddressId != null;
        if (!resumingDraft) {
            guestWizardDraftStore.clearBookingDraft(session, bookingId, guestId);
        }
        GuestForm form = guestWizardDraftStore.getBookingDraft(session, bookingId, guestId)
            .filter(draft -> resumingDraft)
            .orElseGet(() -> guestService.getForm(guestId));
        if (selectedAddressId != null) {
            form = form.withAddressId(selectedAddressId);
        }
        return populateForm(
            model,
            bookingId,
            form,
            "/bookings/" + bookingId + "/guests/" + guestId,
            "Editar huésped",
            "Guardar cambios",
            guestId,
            step
        );
    }

    @GetMapping("/bookings/{bookingId}/guests/{guestId}/review")
    public String reviewGuest(
        @PathVariable Long bookingId,
        @PathVariable Long guestId,
        Model model
    ) {
        BookingDetails details = bookingService.getDetails(bookingId);
        Guest guest = details.guests().stream()
            .filter(item -> item.getId().equals(guestId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa persona."));

        return populateReviewForm(
            model,
            bookingId,
            guestId,
            details,
            guest
        );
    }

    @PostMapping("/bookings/{bookingId}/guests/{guestId}")
    public String updateGuest(
        @PathVariable Long bookingId,
        @PathVariable Long guestId,
        @Valid @ModelAttribute("guestForm") GuestForm form,
        BindingResult bindingResult,
        HttpSession session,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateForm(model, bookingId, form, "/bookings/" + bookingId + "/guests/" + guestId, "Editar huésped", "Guardar cambios", guestId, 3);
        }

        try {
            guestService.update(guestId, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("guest.invalid", exception.getMessage());
            return populateForm(model, bookingId, form, "/bookings/" + bookingId + "/guests/" + guestId, "Editar huésped", "Guardar cambios", guestId, 3);
        }

        guestWizardDraftStore.clearBookingDraft(session, bookingId, guestId);
        redirectAttributes.addFlashAttribute("flashMessage", "Datos del huésped actualizados.");
        redirectAttributes.addFlashAttribute("flashKind", "success");
        return "redirect:/bookings/" + bookingId;
    }

    @PostMapping("/bookings/{bookingId}/guests/{guestId}/review")
    public String approveReviewedGuest(
        @PathVariable Long bookingId,
        @PathVariable Long guestId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            guestService.markReviewed(guestId);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("flashMessage", exception.getMessage());
            return "redirect:/bookings/" + bookingId;
        }

        BookingDetails updatedDetails = bookingService.getDetails(bookingId);
        if (updatedDetails.readyForTravelerPart()) {
            redirectAttributes.addFlashAttribute("flashMessage", "Datos guardados. La estancia ya está lista para descargar el archivo para SES.");
        } else {
            redirectAttributes.addFlashAttribute("flashMessage", "Datos guardados.");
        }
        redirectAttributes.addFlashAttribute("flashKind", "success");
        return "redirect:/bookings/" + bookingId;
    }

    @PostMapping("/bookings/{bookingId}/guests/{guestId}/draft-address")
    public String saveExistingGuestDraftBeforeAddress(
        @PathVariable Long bookingId,
        @PathVariable Long guestId,
        @ModelAttribute("guestForm") GuestForm form,
        HttpSession session
    ) {
        guestWizardDraftStore.saveBookingDraft(session, bookingId, guestId, form);
        return "redirect:/bookings/" + bookingId + "/addresses/new?guestId=" + guestId;
    }

    @GetMapping("/bookings/{bookingId}/traveler-part.xml")
    public Object downloadTravelerPart(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            String xml = travelerPartService.generateXml(bookingId);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                    .filename("parte-viajeros-" + bookingId + ".xml")
                    .build()
                    .toString())
                .body(xml);
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("flashMessage", exception.getMessage());
            return "redirect:/bookings/" + bookingId;
        }
    }

    @PostMapping("/bookings/{bookingId}/submit-to-ses")
    public String submitTravelerPartToSes(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            var result = travelerPartService.submitTravelerPart(bookingId);
            String lote = result.loteCode() == null || result.loteCode().isBlank() ? "sin lote devuelto" : result.loteCode();
            redirectAttributes.addFlashAttribute("flashMessage", "Envío enviado a SES. Lote: " + lote + ".");
            redirectAttributes.addFlashAttribute("flashKind", "success");
        } catch (IllegalStateException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("flashMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("flashKind", "error");
        }
        return "redirect:/bookings/" + bookingId;
    }

    @PostMapping("/bookings/{bookingId}/communications/{communicationId}/refresh-ses-status")
    public String refreshSesSubmissionStatus(
        @PathVariable Long bookingId,
        @PathVariable Long communicationId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            var result = travelerPartService.refreshSesSubmissionStatus(bookingId, communicationId);
            if (result.communicationCode() != null && !result.communicationCode().isBlank()) {
                redirectAttributes.addFlashAttribute("flashMessage", "SES ya ha procesado la comunicación. Código: " + result.communicationCode() + ".");
                redirectAttributes.addFlashAttribute("flashKind", "success");
            } else if (result.processingErrorDescription() != null && !result.processingErrorDescription().isBlank()) {
                redirectAttributes.addFlashAttribute("flashMessage", result.processingErrorDescription());
                redirectAttributes.addFlashAttribute("flashKind", "error");
            } else {
                redirectAttributes.addFlashAttribute("flashMessage", "SES todavía no ha devuelto un código final para este lote.");
                redirectAttributes.addFlashAttribute("flashKind", "success");
            }
        } catch (IllegalStateException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("flashMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("flashKind", "error");
        }
        return "redirect:/bookings/" + bookingId;
    }

    @PostMapping("/bookings/{bookingId}/communications/{communicationId}/cancel-ses")
    public String cancelSesSubmission(
        @PathVariable Long bookingId,
        @PathVariable Long communicationId,
        RedirectAttributes redirectAttributes
    ) {
        try {
            var result = travelerPartService.cancelSesSubmission(bookingId, communicationId);
            redirectAttributes.addFlashAttribute("flashMessage", "Lote anulado en SES.");
            redirectAttributes.addFlashAttribute("flashKind", "success");
        } catch (IllegalStateException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("flashMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("flashKind", "error");
        }
        return "redirect:/bookings/" + bookingId;
    }

    @GetMapping("/bookings/{bookingId}/communications/{communicationId}.xml")
    public ResponseEntity<String> downloadGeneratedCommunication(
        @PathVariable Long bookingId,
        @PathVariable Long communicationId
    ) {
        String xml = travelerPartService.getGeneratedXml(bookingId, communicationId);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("parte-viajeros-" + bookingId + "-" + communicationId + ".xml")
                .build()
                .toString())
            .body(xml);
    }

    @PostMapping("/bookings/{bookingId}/self-service-link")
    public String issueSelfServiceLink(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        guestSelfServiceService.issueAccess(bookingId);
        redirectAttributes.addFlashAttribute("flashMessage", "Enlace para rellenar datos preparado.");
        redirectAttributes.addFlashAttribute("flashKind", "success");
        return "redirect:/bookings/" + bookingId;
    }

    @PostMapping("/bookings/{bookingId}/share-self-service-link")
    public String prepareShareSelfServiceLink(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        BookingDetails details = bookingService.getDetails(bookingId);
        SelfServiceAccess access = details.selfServiceAccess()
            .orElseGet(() -> guestSelfServiceService.issueAccess(bookingId));

        String linkUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/guest-access/{token}")
            .buildAndExpand(access.token())
            .toUriString();
        String message = "Hola.\n\n"
            + "Necesitamos que completes los datos de las personas que se alojan en "
            + details.booking().getAccommodation().getName()
            + ". Solo tarda 1-2 minutos.\n\n"
            + "Hazlo desde aquí:\n"
            + linkUrl;

        redirectAttributes.addFlashAttribute(
            "shareMessage",
            new GuestLinkShareMessage(
                linkUrl,
                message,
                "https://wa.me/?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8)
            )
        );
        return "redirect:/bookings/" + bookingId;
    }

    @PostMapping("/bookings/{bookingId}/self-service-link/revoke")
    public String revokeSelfServiceLink(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        guestSelfServiceService.revokeAccess(bookingId);
        redirectAttributes.addFlashAttribute("flashMessage", "Enlace desactivado.");
        redirectAttributes.addFlashAttribute("flashKind", "success");
        return "redirect:/bookings/" + bookingId;
    }

    private String populateForm(
        Model model,
        Long bookingId,
        GuestForm form,
        String action,
        String title,
        String submitLabel,
        Long guestId,
        Integer step
    ) {
        var details = bookingService.getDetails(bookingId);
        model.addAttribute("details", details);
        model.addAttribute("guestForm", form);
        model.addAttribute("documentTypes", DocumentType.values());
        model.addAttribute("countries", GuestFormOptions.countries());
        model.addAttribute("relationships", GuestRelationship.values());
        model.addAttribute("addresses", addressService.findByBookingId(bookingId));
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
        model.addAttribute("submitLabel", submitLabel);
        model.addAttribute("bookingUrl", "/bookings/" + bookingId);
        model.addAttribute("initialStep", step == null ? null : Math.max(step - 1, 0));
        String newAddressUrl = guestId == null
            ? "/bookings/" + bookingId + "/addresses/new"
            : "/bookings/" + bookingId + "/addresses/new?guestId=" + guestId;
        model.addAttribute("newAddressUrl", newAddressUrl);
        String saveDraftAction = guestId == null
            ? "/bookings/" + bookingId + "/guests/draft-address"
            : "/bookings/" + bookingId + "/guests/" + guestId + "/draft-address";
        model.addAttribute("saveAddressDraftAction", saveDraftAction);
        return "guests/form";
    }

    private String populateReviewForm(
        Model model,
        Long bookingId,
        Long guestId,
        BookingDetails details,
        Guest guest
    ) {
        List<String> reviewIssues = buildReviewIssues(guest, details.booking().getCheckInDate());
        model.addAttribute("details", details);
        model.addAttribute("guest", guest);
        model.addAttribute("bookingUrl", "/bookings/" + bookingId);
        model.addAttribute("reviewAction", "/bookings/" + bookingId + "/guests/" + guestId + "/review");
        model.addAttribute("editUrl", "/bookings/" + bookingId + "/guests/" + guestId + "/edit");
        model.addAttribute("nextPendingGuestId", findNextPendingGuestId(details, guestId));
        model.addAttribute("reviewIssues", reviewIssues);
        model.addAttribute("reviewLeadIssue", reviewIssues.getFirst());
        model.addAttribute("showRelationship", isMinorAtCheckIn(guest, details.booking().getCheckInDate()) && !isBlank(guest.getRelationship()));
        model.addAttribute("relationshipDisplay", buildRelationshipDisplay(guest));
        model.addAttribute("showMunicipalityCode", showsMunicipalityCode(guest));
        model.addAttribute("municipalityCodeDisplay", buildMunicipalityCodeDisplay(guest));
        model.addAttribute("municipalityNameDisplay", buildMunicipalityNameDisplay(guest));
        return "guests/review";
    }

    private Long findNextPendingGuestId(BookingDetails details, Long currentGuestId) {
        return details.guests().stream()
            .filter(guest -> guest.getReviewStatus() == GuestReviewStatus.PENDING_REVIEW)
            .filter(guest -> !guest.getId().equals(currentGuestId))
            .map(Guest::getId)
            .findFirst()
            .orElse(null);
    }

    private List<String> buildReviewIssues(Guest guest, LocalDate checkInDate) {
        List<String> issues = new ArrayList<>();

        if (requiresDocument(guest, checkInDate) && (guest.getDocumentType() == null || isBlank(guest.getDocumentNumber()))) {
            issues.add("Falta revisar el documento.");
        }
        if (guest.getBirthDate() == null) {
            issues.add("Falta la fecha de nacimiento.");
        }
        if (isBlank(guest.getPhone()) && isBlank(guest.getPhone2()) && isBlank(guest.getEmail())) {
            issues.add("Falta un teléfono o un correo.");
        }
        if ("ESP".equalsIgnoreCase(guest.getCountry()) && isBlank(guest.getMunicipalityCode())) {
            issues.add("Falta indicar el municipio.");
        }
        if (guest.getAddress() == null || isBlank(guest.getAddressLine()) || isBlank(guest.getPostalCode())) {
            issues.add("Falta revisar la dirección.");
        }
        if (isMinorAtCheckIn(guest, checkInDate) && isBlank(guest.getRelationship())) {
            issues.add("Falta indicar el parentesco.");
        }
        if (issues.isEmpty()) {
            issues.add("Solo confirma que todo está correcto y guarda.");
        }
        return issues;
    }

    private boolean requiresDocument(Guest guest, LocalDate checkInDate) {
        if (guest.getBirthDate() == null) {
            return true;
        }
        int age = checkInDate.getYear() - guest.getBirthDate().getYear();
        if (checkInDate.getDayOfYear() < guest.getBirthDate().getDayOfYear()) {
            age--;
        }
        return age >= 18 || ("ESP".equalsIgnoreCase(guest.getNationality()) && age >= 14);
    }

    private boolean isMinorAtCheckIn(Guest guest, LocalDate checkInDate) {
        if (guest.getBirthDate() == null) {
            return false;
        }
        int age = checkInDate.getYear() - guest.getBirthDate().getYear();
        if (checkInDate.getDayOfYear() < guest.getBirthDate().getDayOfYear()) {
            age--;
        }
        return age < 18;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean showsMunicipalityCode(Guest guest) {
        return "ESP".equalsIgnoreCase(guest.getCountry());
    }

    private String buildMunicipalityCodeDisplay(Guest guest) {
        return isBlank(guest.getMunicipalityCode()) ? "Sin código" : guest.getMunicipalityCode();
    }

    private String buildMunicipalityNameDisplay(Guest guest) {
        return isBlank(guest.getMunicipalityName()) ? "Sin municipio" : guest.getMunicipalityName();
    }

    private String buildRelationshipDisplay(Guest guest) {
        if (isBlank(guest.getRelationship())) {
            return "";
        }
        for (GuestRelationship relationship : GuestRelationship.values()) {
            if (relationship.name().equalsIgnoreCase(guest.getRelationship())) {
                return relationship.getLabel();
            }
        }
        return guest.getRelationship();
    }
}
