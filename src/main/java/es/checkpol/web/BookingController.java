package es.checkpol.web;

import es.checkpol.service.AccommodationService;
import es.checkpol.service.BookingFilter;
import es.checkpol.service.BookingListItem;
import es.checkpol.service.BookingService;
import es.checkpol.domain.CommunicationDispatchStatus;
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
        List<BookingListItem> allItems = bookingService.findAll();
        List<BookingListItem> incompleteItems = bookingService.findAll(BookingFilter.INCOMPLETE);
        List<BookingListItem> readyItems = bookingService.findAll(BookingFilter.READY);
        List<BookingListItem> todayItems = bookingService.findAll(BookingFilter.TODAY);
        List<BookingListItem> upcomingItems = bookingService.findAll(BookingFilter.UPCOMING);
        List<BookingListItem> reviewQueue = incompleteItems.stream()
            .limit(3)
            .toList();
        List<BookingListItem> communicationActionItems = readyItems.stream()
            .filter(BookingListItem::needsCommunicationAction)
            .toList();
        BookingListItem nextCommunicationBooking = communicationActionItems.isEmpty() ? null : communicationActionItems.getFirst();

        model.addAttribute("bookings", switch (selectedFilter) {
            case ALL -> allItems;
            case INCOMPLETE -> incompleteItems;
            case READY -> readyItems;
            case TODAY -> todayItems;
            case UPCOMING -> upcomingItems;
        });
        model.addAttribute("reviewQueue", reviewQueue);
        model.addAttribute("nextBooking", incompleteItems.isEmpty() ? null : incompleteItems.getFirst());
        model.addAttribute("nextCommunicationBooking", nextCommunicationBooking);
        model.addAttribute("nextCommunicationSummary", nextCommunicationBooking == null ? null : buildNextCommunicationSummary(nextCommunicationBooking));
        model.addAttribute("selectedFilter", selectedFilter.name());
        model.addAttribute("hasAccommodations", !accommodations.isEmpty());
        model.addAttribute("countAll", allItems.size());
        model.addAttribute("countIncomplete", incompleteItems.size());
        model.addAttribute("countCommunicationPending", communicationActionItems.size());
        model.addAttribute("countReady", readyItems.size());
        model.addAttribute("countToday", todayItems.size());
        model.addAttribute("countUpcoming", upcomingItems.size());
        model.addAttribute("reviewSummary", buildReviewSummary(incompleteItems.size()));
        model.addAttribute("communicationSummary", buildCommunicationSummary(readyItems, communicationActionItems));
        return "bookings/list";
    }

    private BookingSummaryCard buildReviewSummary(int incompleteCount) {
        String title = incompleteCount == 1
            ? "1 estancia pendiente"
            : incompleteCount + " estancias pendientes";
        String text = incompleteCount == 0
            ? "No hay datos bloqueados por revisar."
            : "Revísalas antes de presentar o descargar el parte.";
        return new BookingSummaryCard(title, text);
    }

    private BookingSummaryCard buildCommunicationSummary(List<BookingListItem> readyItems, List<BookingListItem> communicationActionItems) {
        int pendingCount = communicationActionItems.size();
        if (pendingCount == 0) {
            String title = readyItems.size() == 1
                ? "1 estancia sin bloqueos"
                : readyItems.size() + " estancias sin bloqueos";
            String text = readyItems.isEmpty()
                ? "Cuando completes huéspedes, aquí verás los partes listos."
                : "No hay partes pendientes ahora. Entra solo si necesitas consultar o corregir una estancia.";
            return new BookingSummaryCard(title, text);
        }

        boolean allReadyToSubmit = communicationActionItems.stream()
            .allMatch(this::isReadyToSubmitBySes);
        boolean allReadyToDownload = communicationActionItems.stream()
            .allMatch(this::isReadyToDownloadManually);
        long sesPendingCount = communicationActionItems.stream()
            .filter(BookingListItem::sesSubmissionAvailable)
            .count();
        long manualPendingCount = pendingCount - sesPendingCount;

        if (allReadyToSubmit) {
            String title = pendingCount == 1
                ? "1 parte pendiente de presentar"
                : pendingCount + " partes pendientes de presentar";
            String text = pendingCount == 1
                ? "Abre la estancia destacada y presenta el parte en SES."
                : "Entra en cada estancia y presenta el parte en SES.";
            return new BookingSummaryCard(title, text);
        }

        if (allReadyToDownload) {
            String title = pendingCount == 1
                ? "1 XML pendiente de descargar"
                : pendingCount + " XML pendientes de descargar";
            String text = pendingCount == 1
                ? "Abre la estancia y descarga el archivo para SES."
                : "Entra en cada estancia y descarga el archivo para SES.";
            return new BookingSummaryCard(title, text);
        }

        if (sesPendingCount == pendingCount) {
            String title = pendingCount == 1
                ? "1 parte requiere acción"
                : pendingCount + " partes requieren acción";
            String text = pendingCount == 1
                ? "Abre la estancia. Checkpol te dirá si toca presentar, corregir o revisar SES."
                : "Entra en cada estancia. Checkpol te dirá si toca presentar, corregir o revisar SES.";
            return new BookingSummaryCard(title, text);
        }

        if (manualPendingCount == pendingCount) {
            String title = pendingCount == 1
                ? "1 XML requiere acción"
                : pendingCount + " XML requieren acción";
            String text = pendingCount == 1
                ? "Abre la estancia y revisa el siguiente paso del XML."
                : "Entra en cada estancia y revisa el siguiente paso del XML.";
            return new BookingSummaryCard(title, text);
        }

        String title = pendingCount == 1
            ? "1 parte preparado"
            : pendingCount + " partes preparados";
        return new BookingSummaryCard(
            title,
            "Entra en cada estancia. Checkpol indicará si toca presentar en SES o descargar XML."
        );
    }

    private BookingSummaryCard buildNextCommunicationSummary(BookingListItem item) {
        String accommodationName = item.booking().getAccommodation().getName();
        CommunicationDispatchStatus status = item.communicationDispatchStatus();

        if (isReadyToSubmitBySes(item)) {
            return new BookingSummaryCard(
                "Presenta el parte en SES",
                accommodationName + " · Datos revisados. Falta presentar el parte de viajeros."
            );
        }
        if (isReadyToDownloadManually(item)) {
            return new BookingSummaryCard(
                "Descarga el archivo para SES",
                accommodationName + " · Datos revisados. Falta descargar el XML."
            );
        }
        if (status == CommunicationDispatchStatus.SES_CANCELLED) {
            return new BookingSummaryCard(
                "Revisa antes de reenviar",
                accommodationName + " · Comunicación anulada. Abre la estancia para ver el siguiente paso."
            );
        }
        if (status == CommunicationDispatchStatus.SUBMISSION_FAILED
            || status == CommunicationDispatchStatus.SES_PROCESSING_ERROR) {
            return new BookingSummaryCard(
                "Revisa incidencia SES",
                accommodationName + " · Hay una respuesta SES que revisar antes de continuar."
            );
        }
        return new BookingSummaryCard(
            "Abre la estancia",
            accommodationName + " · Checkpol te indicará el siguiente paso."
        );
    }

    private boolean isReadyToSubmitBySes(BookingListItem item) {
        CommunicationDispatchStatus status = item.communicationDispatchStatus();
        return item.sesSubmissionAvailable()
            && (status == null || status == CommunicationDispatchStatus.XML_READY);
    }

    private boolean isReadyToDownloadManually(BookingListItem item) {
        CommunicationDispatchStatus status = item.communicationDispatchStatus();
        return !item.sesSubmissionAvailable()
            && (status == null || status == CommunicationDispatchStatus.XML_READY);
    }

    @GetMapping("/bookings/new")
    public String newBooking(Model model) {
        return populateForm(model, new BookingForm(), "/bookings", "/bookings", "Nueva estancia", "Guardar estancia");
    }

    @PostMapping("/bookings")
    public String createBooking(
        @Valid @ModelAttribute("bookingForm") BookingForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateForm(model, form, "/bookings", "/bookings", "Nueva estancia", "Guardar estancia");
        }

        try {
            bookingService.create(form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("booking.invalid", exception.getMessage());
            return populateForm(model, form, "/bookings", "/bookings", "Nueva estancia", "Guardar estancia");
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Estancia guardada correctamente.");
        redirectAttributes.addFlashAttribute("flashKind", "success");
        return "redirect:/bookings";
    }

    @GetMapping("/bookings/{id}/edit")
    public String editBooking(@PathVariable Long id, Model model) {
        return populateForm(model, bookingService.getForm(id), "/bookings/" + id, "/bookings/" + id, "Editar estancia", "Guardar cambios");
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
            return populateForm(model, form, "/bookings/" + id, "/bookings/" + id, "Editar estancia", "Guardar cambios");
        }

        try {
            bookingService.update(id, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("booking.invalid", exception.getMessage());
            return populateForm(model, form, "/bookings/" + id, "/bookings/" + id, "Editar estancia", "Guardar cambios");
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Estancia actualizada correctamente.");
        redirectAttributes.addFlashAttribute("flashKind", "success");
        return "redirect:/bookings/" + id;
    }

    private String populateForm(Model model, BookingForm form, String action, String backHref, String title, String submitLabel) {
        List<es.checkpol.domain.Accommodation> accommodations = accommodationService.findAll();
        model.addAttribute("bookingForm", selectAccommodationIfOnlyOption(form, accommodations));
        model.addAttribute("accommodations", accommodations);
        model.addAttribute("formAction", action);
        model.addAttribute("backHref", backHref);
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

    public record BookingSummaryCard(String title, String text) {
    }
}
