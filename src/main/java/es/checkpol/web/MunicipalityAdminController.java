package es.checkpol.web;

import es.checkpol.service.CurrentAppUserService;
import es.checkpol.service.MunicipalityAdminService;
import es.checkpol.service.MunicipalityCatalogImportService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MunicipalityAdminController {

    private final MunicipalityAdminService municipalityAdminService;
    private final CurrentAppUserService currentAppUserService;

    public MunicipalityAdminController(
        MunicipalityAdminService municipalityAdminService,
        CurrentAppUserService currentAppUserService
    ) {
        this.municipalityAdminService = municipalityAdminService;
        this.currentAppUserService = currentAppUserService;
    }

    @GetMapping("/admin/municipalities")
    public String municipalities(Model model) {
        if (!model.containsAttribute("municipalityImportForm")) {
            model.addAttribute("municipalityImportForm", municipalityAdminService.defaultForm());
        }
        return populatePage(model, null);
    }

    @PostMapping("/admin/municipalities/preview")
    public String preview(
        @Valid @ModelAttribute("municipalityImportForm") AdminMunicipalityImportForm form,
        BindingResult bindingResult,
        Model model
    ) {
        MunicipalityCatalogImportService.PreviewSummary preview = null;
        if (!bindingResult.hasErrors()) {
            try {
                preview = municipalityAdminService.previewImport(form);
            } catch (IllegalArgumentException exception) {
                bindingResult.reject("municipality.preview.invalid", exception.getMessage());
            }
        }
        return populatePage(model, preview);
    }

    @PostMapping("/admin/municipalities/import")
    public String importCatalog(
        @Valid @ModelAttribute("municipalityImportForm") AdminMunicipalityImportForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populatePage(model, null);
        }

        try {
            MunicipalityCatalogImportService.ImportSummary summary = municipalityAdminService.importCatalog(form, currentAppUserService.requireAuthenticatedUser().getUsername());
            redirectAttributes.addFlashAttribute(
                "flashMessage",
                "Catálogo importado correctamente. Municipios: " + summary.importedMunicipalities() + ", mappings postales: " + summary.importedPostalMappings() + "."
            );
            redirectAttributes.addFlashAttribute("flashKind", "success");
            return "redirect:/admin/municipalities";
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("municipality.import.invalid", exception.getMessage());
            MunicipalityCatalogImportService.PreviewSummary preview = null;
            try {
                preview = municipalityAdminService.previewImport(form);
            } catch (RuntimeException ignored) {
            }
            return populatePage(model, preview);
        }
    }

    private String populatePage(Model model, MunicipalityCatalogImportService.PreviewSummary preview) {
        model.addAttribute("dashboard", municipalityAdminService.getDashboardSummary());
        model.addAttribute("preview", preview);
        return "admin/municipalities";
    }
}
