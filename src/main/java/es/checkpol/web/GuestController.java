package es.checkpol.web;

import es.checkpol.domain.DocumentType;
import es.checkpol.domain.GuestSex;
import es.checkpol.service.BookingService;
import es.checkpol.service.GuestService;
import es.checkpol.service.GuestSelfServiceService;
import es.checkpol.service.TravelerPartService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GuestController {

    private final BookingService bookingService;
    private final GuestService guestService;
    private final TravelerPartService travelerPartService;
    private final GuestSelfServiceService guestSelfServiceService;

    public GuestController(
        BookingService bookingService,
        GuestService guestService,
        TravelerPartService travelerPartService,
        GuestSelfServiceService guestSelfServiceService
    ) {
        this.bookingService = bookingService;
        this.guestService = guestService;
        this.travelerPartService = travelerPartService;
        this.guestSelfServiceService = guestSelfServiceService;
    }

    @GetMapping("/bookings/{bookingId}")
    public String details(@PathVariable Long bookingId, Model model) {
        model.addAttribute("details", bookingService.getDetails(bookingId));
        return "bookings/details";
    }

    @GetMapping("/bookings/{bookingId}/guests/new")
    public String newGuest(@PathVariable Long bookingId, Model model) {
        return populateForm(model, bookingId, new GuestForm(), "/bookings/" + bookingId + "/guests", "Nuevo huesped", "Guardar");
    }

    @PostMapping("/bookings/{bookingId}/guests")
    public String createGuest(
        @PathVariable Long bookingId,
        @Valid @ModelAttribute("guestForm") GuestForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateForm(model, bookingId, form, "/bookings/" + bookingId + "/guests", "Nuevo huesped", "Guardar");
        }

        try {
            guestService.create(bookingId, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("guest.invalid", exception.getMessage());
            return populateForm(model, bookingId, form, "/bookings/" + bookingId + "/guests", "Nuevo huesped", "Guardar");
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Datos del huesped guardados.");
        return "redirect:/bookings/" + bookingId;
    }

    @GetMapping("/bookings/{bookingId}/guests/{guestId}/edit")
    public String editGuest(@PathVariable Long bookingId, @PathVariable Long guestId, Model model) {
        return populateForm(
            model,
            bookingId,
            guestService.getForm(guestId),
            "/bookings/" + bookingId + "/guests/" + guestId,
            "Editar huesped",
            "Guardar cambios"
        );
    }

    @PostMapping("/bookings/{bookingId}/guests/{guestId}")
    public String updateGuest(
        @PathVariable Long bookingId,
        @PathVariable Long guestId,
        @Valid @ModelAttribute("guestForm") GuestForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateForm(model, bookingId, form, "/bookings/" + bookingId + "/guests/" + guestId, "Editar huesped", "Guardar cambios");
        }

        try {
            guestService.update(guestId, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("guest.invalid", exception.getMessage());
            return populateForm(model, bookingId, form, "/bookings/" + bookingId + "/guests/" + guestId, "Editar huesped", "Guardar cambios");
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Datos del huesped actualizados.");
        return "redirect:/bookings/" + bookingId;
    }

    @GetMapping("/bookings/{bookingId}/traveler-part.xml")
    public ResponseEntity<String> downloadTravelerPart(@PathVariable Long bookingId) {
        String xml = travelerPartService.generateXml(bookingId);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("parte-viajeros-" + bookingId + ".xml")
                .build()
                .toString())
            .body(xml);
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
    public String markGuestReviewed(@PathVariable Long bookingId, @PathVariable Long guestId, RedirectAttributes redirectAttributes) {
        guestService.markReviewed(guestId);
        redirectAttributes.addFlashAttribute("flashMessage", "Huesped revisado.");
        return "redirect:/bookings/" + bookingId;
    }

    private String populateForm(Model model, Long bookingId, GuestForm form, String action, String title, String submitLabel) {
        model.addAttribute("details", bookingService.getDetails(bookingId));
        model.addAttribute("guestForm", form);
        model.addAttribute("documentTypes", DocumentType.values());
        model.addAttribute("sexes", GuestSex.values());
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
        model.addAttribute("submitLabel", submitLabel);
        return "guests/form";
    }
}
