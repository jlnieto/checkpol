package es.checkpol.web;

import es.checkpol.service.AccommodationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccommodationController {

    private final AccommodationService accommodationService;

    public AccommodationController(AccommodationService accommodationService) {
        this.accommodationService = accommodationService;
    }

    @GetMapping("/accommodations/new")
    public String newAccommodation(Model model) {
        return populateForm(model, new AccommodationForm(), "/accommodations", "Nueva vivienda", "Guardar vivienda");
    }

    @PostMapping("/accommodations")
    public String createAccommodation(
        @Valid @ModelAttribute("accommodationForm") AccommodationForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateForm(model, form, "/accommodations", "Nueva vivienda", "Guardar vivienda");
        }

        accommodationService.create(form);
        redirectAttributes.addFlashAttribute("flashMessage", "Vivienda guardada correctamente.");
        return "redirect:/bookings";
    }

    @GetMapping("/accommodations/{id}/edit")
    public String editAccommodation(@PathVariable Long id, Model model) {
        return populateForm(model, accommodationService.getForm(id), "/accommodations/" + id, "Editar vivienda", "Guardar cambios");
    }

    @PostMapping("/accommodations/{id}")
    public String updateAccommodation(
        @PathVariable Long id,
        @Valid @ModelAttribute("accommodationForm") AccommodationForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateForm(model, form, "/accommodations/" + id, "Editar vivienda", "Guardar cambios");
        }

        accommodationService.update(id, form);
        redirectAttributes.addFlashAttribute("flashMessage", "Vivienda actualizada correctamente.");
        return "redirect:/bookings";
    }

    private String populateForm(Model model, AccommodationForm form, String action, String title, String submitLabel) {
        model.addAttribute("accommodationForm", form);
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
        model.addAttribute("submitLabel", submitLabel);
        return "accommodations/form";
    }
}
