package es.checkpol.web;

import es.checkpol.domain.DocumentType;
import es.checkpol.domain.GuestSex;
import es.checkpol.service.GuestSelfServiceService;
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
public class GuestSelfServiceController {

    private final GuestSelfServiceService guestSelfServiceService;

    public GuestSelfServiceController(GuestSelfServiceService guestSelfServiceService) {
        this.guestSelfServiceService = guestSelfServiceService;
    }

    @GetMapping("/guest-access/{token}")
    public String showForm(@PathVariable String token, Model model) {
        populate(model, token, new GuestForm());
        return "public/guest-form";
    }

    @PostMapping("/guest-access/{token}")
    public String submit(
        @PathVariable String token,
        @Valid @ModelAttribute("guestForm") GuestForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populate(model, token, form);
            return "public/guest-form";
        }

        try {
            guestSelfServiceService.submitGuest(token, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("guest.invalid", exception.getMessage());
            populate(model, token, form);
            return "public/guest-form";
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Tus datos se han enviado correctamente.");
        return "redirect:/guest-access/" + token;
    }

    @GetMapping("/guest-access/{token}/guests/{guestId}/edit")
    public String edit(@PathVariable String token, @PathVariable Long guestId, Model model) {
        populate(model, token, guestSelfServiceService.getGuestForm(token, guestId));
        model.addAttribute("publicEditGuestId", guestId);
        return "public/guest-form";
    }

    @PostMapping("/guest-access/{token}/guests/{guestId}")
    public String update(
        @PathVariable String token,
        @PathVariable Long guestId,
        @Valid @ModelAttribute("guestForm") GuestForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populate(model, token, form);
            model.addAttribute("publicEditGuestId", guestId);
            return "public/guest-form";
        }

        try {
            guestSelfServiceService.updateGuest(token, guestId, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("guest.invalid", exception.getMessage());
            populate(model, token, form);
            model.addAttribute("publicEditGuestId", guestId);
            return "public/guest-form";
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Tus datos se han actualizado correctamente.");
        return "redirect:/guest-access/" + token;
    }

    private void populate(Model model, String token, GuestForm form) {
        model.addAttribute("access", guestSelfServiceService.getByToken(token));
        model.addAttribute("guestForm", form);
        model.addAttribute("documentTypes", DocumentType.values());
        model.addAttribute("sexes", GuestSex.values());
    }
}
