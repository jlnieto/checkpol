package es.checkpol.web;

import es.checkpol.service.AccommodationService;
import es.checkpol.service.BookingFilter;
import es.checkpol.service.BookingService;
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
import java.util.Objects;

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
        List<es.checkpol.domain.Accommodation> accommodations = accommodationService.findAll();
        List<es.checkpol.service.BookingListItem> allItems = bookingService.findAll();
        List<es.checkpol.service.BookingListItem> incompleteItems = bookingService.findAll(BookingFilter.INCOMPLETE);
        List<es.checkpol.service.BookingListItem> readyItems = bookingService.findAll(BookingFilter.READY);
        List<es.checkpol.service.BookingListItem> todayItems = bookingService.findAll(BookingFilter.TODAY);
        List<es.checkpol.service.BookingListItem> upcomingItems = bookingService.findAll(BookingFilter.UPCOMING);
        List<es.checkpol.service.BookingListItem> reviewQueue = incompleteItems.stream()
            .limit(3)
            .toList();

        model.addAttribute("bookings", switch (selectedFilter) {
            case ALL -> allItems;
            case INCOMPLETE -> incompleteItems;
            case READY -> readyItems;
            case TODAY -> todayItems;
            case UPCOMING -> upcomingItems;
        });
        model.addAttribute("reviewQueue", reviewQueue);
        model.addAttribute("nextBooking", incompleteItems.isEmpty() ? null : incompleteItems.getFirst());
        model.addAttribute("selectedFilter", selectedFilter.name());
        model.addAttribute("hasAccommodations", !accommodations.isEmpty());
        model.addAttribute("countAll", allItems.size());
        model.addAttribute("countIncomplete", incompleteItems.size());
        model.addAttribute("countReady", readyItems.size());
        model.addAttribute("countToday", todayItems.size());
        model.addAttribute("countUpcoming", upcomingItems.size());
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
        List<es.checkpol.domain.Accommodation> accommodations = accommodationService.findAll();
        model.addAttribute("bookingForm", selectAccommodationIfOnlyOption(form, accommodations));
        model.addAttribute("accommodations", accommodations);
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
        model.addAttribute("submitLabel", submitLabel);
        return "bookings/form";
    }

    private BookingForm selectAccommodationIfOnlyOption(BookingForm form, List<es.checkpol.domain.Accommodation> accommodations) {
        if (form.accommodationId() != null) {
            return form;
        }

        List<Long> selectableAccommodationIds = accommodations.stream()
            .map(es.checkpol.domain.Accommodation::getId)
            .filter(Objects::nonNull)
            .toList();

        if (selectableAccommodationIds.size() != 1) {
            return form;
        }

        return new BookingForm(
            selectableAccommodationIds.getFirst(),
            form.referenceCode(),
            form.personCount(),
            form.contractDate(),
            form.checkInDate(),
            form.checkOutDate()
        );
    }
}
