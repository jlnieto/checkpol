package es.checkpol.web;

import es.checkpol.domain.DocumentType;
import es.checkpol.domain.GuestRelationship;
import es.checkpol.domain.GuestSex;
import es.checkpol.service.AddressService;
import es.checkpol.service.BookingService;
import es.checkpol.service.GuestService;
import es.checkpol.service.GuestSelfServiceService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

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
        return populateForm(model, bookingId, form, "/bookings/" + bookingId + "/guests", "Nuevo huesped", "Guardar", null, step);
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
            return populateForm(model, bookingId, form, "/bookings/" + bookingId + "/guests", "Nuevo huesped", "Guardar", null, 3);
        }

        try {
            guestService.create(bookingId, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("guest.invalid", exception.getMessage());
            return populateForm(model, bookingId, form, "/bookings/" + bookingId + "/guests", "Nuevo huesped", "Guardar", null, 3);
        }

        guestWizardDraftStore.clearBookingDraft(session, bookingId, null);
        redirectAttributes.addFlashAttribute("flashMessage", "Datos del huesped guardados.");
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
            "Editar huesped",
            "Guardar cambios",
            guestId,
            step
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
            return populateForm(model, bookingId, form, "/bookings/" + bookingId + "/guests/" + guestId, "Editar huesped", "Guardar cambios", guestId, 3);
        }

        try {
            guestService.update(guestId, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("guest.invalid", exception.getMessage());
            return populateForm(model, bookingId, form, "/bookings/" + bookingId + "/guests/" + guestId, "Editar huesped", "Guardar cambios", guestId, 3);
        }

        guestWizardDraftStore.clearBookingDraft(session, bookingId, guestId);
        redirectAttributes.addFlashAttribute("flashMessage", "Datos del huesped actualizados.");
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
        return "redirect:/bookings/" + bookingId;
    }

    @PostMapping("/bookings/{bookingId}/self-service-link/revoke")
    public String revokeSelfServiceLink(@PathVariable Long bookingId, RedirectAttributes redirectAttributes) {
        guestSelfServiceService.revokeAccess(bookingId);
        redirectAttributes.addFlashAttribute("flashMessage", "Enlace desactivado.");
        return "redirect:/bookings/" + bookingId;
    }

    @PostMapping("/bookings/{bookingId}/guests/{guestId}/review")
    public Object markGuestReviewed(
        @PathVariable Long bookingId,
        @PathVariable Long guestId,
        @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
        RedirectAttributes redirectAttributes
    ) {
        try {
            guestService.markReviewed(guestId);
        } catch (IllegalArgumentException exception) {
            if (isAjaxRequest(requestedWith)) {
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                        "success", false,
                        "message", "No se pudo marcar como revisado. Intentalo de nuevo."
                    ));
            }
            redirectAttributes.addFlashAttribute("flashMessage", exception.getMessage());
            return "redirect:/bookings/" + bookingId;
        }

        if (isAjaxRequest(requestedWith)) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "success", true,
                    "guestId", guestId,
                    "message", "Huesped marcado como revisado."
                ));
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Huesped revisado.");
        return "redirect:/bookings/" + bookingId;
    }

    private boolean isAjaxRequest(String requestedWith) {
        return requestedWith != null && !requestedWith.isBlank();
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
        model.addAttribute("sexes", GuestSex.values());
        model.addAttribute("addresses", addressService.findByBookingId(bookingId));
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
        model.addAttribute("submitLabel", submitLabel);
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
}
