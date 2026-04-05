package es.checkpol.web;

import es.checkpol.domain.AppUser;
import es.checkpol.domain.SesConnectionTestStatus;
import es.checkpol.service.OwnerSesSettingsService;
import es.checkpol.service.SesConnectionTestResult;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OwnerSesSettingsController {

    private final OwnerSesSettingsService ownerSesSettingsService;

    public OwnerSesSettingsController(OwnerSesSettingsService ownerSesSettingsService) {
        this.ownerSesSettingsService = ownerSesSettingsService;
    }

    @GetMapping("/bookings/ses-settings")
    public String edit(Model model) {
        return populatePage(model, ownerSesSettingsService.getCurrentForm(), ownerSesSettingsService.getCurrentOwner());
    }

    @PostMapping("/bookings/ses-settings")
    public String update(
        @Valid @ModelAttribute("ownerSesSettingsForm") OwnerSesSettingsForm form,
        BindingResult bindingResult,
        @RequestParam(name = "afterSaveAction", required = false) String afterSaveAction,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populatePage(model, form, ownerSesSettingsService.getCurrentOwner());
        }

        if ("test".equals(afterSaveAction)) {
            SesConnectionTestResult result = ownerSesSettingsService.saveAndTest(form);
            redirectAttributes.addFlashAttribute("flashMessage", result.ownerMessage());
            redirectAttributes.addFlashAttribute("flashKind", flashKindForOwner(result));
            return "redirect:/bookings/ses-settings";
        }

        AppUser owner = ownerSesSettingsService.save(form);
        redirectAttributes.addFlashAttribute("flashMessage", owner.hasSesWebServiceConfiguration()
            ? "Configuración SES guardada."
            : "Configuración guardada. Mientras falten datos de SES, seguirás en modo manual: descargar XML y presentarlo en la web de SES.");
        redirectAttributes.addFlashAttribute("flashKind", owner.hasSesWebServiceConfiguration() ? "success" : "warning");
        return "redirect:/bookings/ses-settings";
    }

    private String populatePage(Model model, OwnerSesSettingsForm form, AppUser owner) {
        boolean hasArrendadorCode = hasText(form.sesArrendadorCode());
        boolean hasWsUsername = hasText(form.sesWsUsername());
        boolean hasVisiblePassword = hasText(form.sesWsPassword());
        boolean hasStoredPassword = hasText(owner.getSesWsPasswordEncrypted());
        boolean sesReady = hasArrendadorCode && hasWsUsername && (hasVisiblePassword || hasStoredPassword);
        boolean sesManualOnly = !hasArrendadorCode && !hasWsUsername && !hasVisiblePassword && !hasStoredPassword;

        model.addAttribute("ownerSesSettingsForm", form);
        model.addAttribute("sesReady", sesReady);
        model.addAttribute("sesManualOnly", sesManualOnly);
        model.addAttribute("sesPasswordStored", hasStoredPassword);
        model.addAttribute("connectionTestStatus", owner.getSesConnectionTestStatus() == null ? SesConnectionTestStatus.NOT_TESTED : owner.getSesConnectionTestStatus());
        model.addAttribute("connectionTestedAt", owner.getSesConnectionTestedAt());
        model.addAttribute("connectionMessage", owner.getSesConnectionOwnerMessage());
        return "bookings/ses-settings";
    }

    private String flashKindForOwner(SesConnectionTestResult result) {
        if (result.success()) {
            return "success";
        }
        return result.technicalIssue() ? "error" : "warning";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
