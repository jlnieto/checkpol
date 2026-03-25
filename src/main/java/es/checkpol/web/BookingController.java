package es.checkpol.web;

import es.checkpol.service.AccommodationService;
import es.checkpol.service.BookingFilter;
import es.checkpol.service.BookingService;
import es.checkpol.domain.PaymentType;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class BookingController {

    private final BookingService bookingService;
    private final AccommodationService accommodationService;

    public BookingController(BookingService bookingService, AccommodationService accommodationService) {
        this.bookingService = bookingService;
        this.accommodationService = accommodationService;
    }

    @GetMapping("/bookings")
    public String list(@RequestParam(name = "filter", required = false) String filter, Model model) {
        BookingFilter selectedFilter = BookingFilter.fromParam(filter);
        List<es.checkpol.service.BookingListItem> allItems = bookingService.findAll();
        model.addAttribute("bookings", bookingService.findAll(selectedFilter));
        model.addAttribute("selectedFilter", selectedFilter.name());
        model.addAttribute("countAll", allItems.size());
        model.addAttribute("countIncomplete", allItems.stream().filter(item -> !item.readyForTravelerPart()).count());
        model.addAttribute("countReady", allItems.stream().filter(es.checkpol.service.BookingListItem::readyForTravelerPart).count());
        java.time.LocalDate today = java.time.LocalDate.now();
        model.addAttribute("countToday", allItems.stream().filter(item -> item.booking().getCheckInDate().isEqual(today)).count());
        model.addAttribute("countUpcoming", allItems.stream().filter(item -> item.booking().getCheckInDate().isAfter(today)).count());
        return "bookings/list";
    }

    @GetMapping("/bookings/new")
    public String newBooking(Model model) {
        return populateForm(model, new BookingForm(), "/bookings", "Nueva estancia", "Guardar estancia");
    }

    @PostMapping("/bookings")
    public String createBooking(
        @Valid @ModelAttribute("bookingForm") BookingForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateForm(model, form, "/bookings", "Nueva estancia", "Guardar estancia");
        }

        try {
            bookingService.create(form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("booking.invalid", exception.getMessage());
            return populateForm(model, form, "/bookings", "Nueva estancia", "Guardar estancia");
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Estancia guardada correctamente.");
        return "redirect:/bookings";
    }

    @GetMapping("/bookings/{id}/edit")
    public String editBooking(@PathVariable Long id, Model model) {
        return populateForm(model, bookingService.getForm(id), "/bookings/" + id, "Editar estancia", "Guardar cambios");
    }

    @PostMapping("/bookings/{id}")
    public String updateBooking(
        @PathVariable Long id,
        @Valid @ModelAttribute("bookingForm") BookingForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateForm(model, form, "/bookings/" + id, "Editar estancia", "Guardar cambios");
        }

        try {
            bookingService.update(id, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("booking.invalid", exception.getMessage());
            return populateForm(model, form, "/bookings/" + id, "Editar estancia", "Guardar cambios");
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Estancia actualizada correctamente.");
        return "redirect:/bookings/" + id;
    }

    private String populateForm(Model model, BookingForm form, String action, String title, String submitLabel) {
        model.addAttribute("bookingForm", form);
        model.addAttribute("accommodations", accommodationService.findAll());
        model.addAttribute("channels", es.checkpol.domain.BookingChannel.values());
        model.addAttribute("paymentTypes", PaymentType.values());
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
        model.addAttribute("submitLabel", submitLabel);
        return "bookings/form";
    }
}
