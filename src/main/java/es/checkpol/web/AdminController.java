package es.checkpol.web;

import es.checkpol.service.MunicipalityReviewService;
import es.checkpol.service.MunicipalityAdminDashboard;
import es.checkpol.service.MunicipalityIssueSummary;
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

import java.util.Comparator;

@Controller
public class AdminController {

    private final MunicipalityReviewService municipalityReviewService;

    public AdminController(MunicipalityReviewService municipalityReviewService) {
        this.municipalityReviewService = municipalityReviewService;
    }

    @GetMapping("/admin")
    public String adminHome() {
        return "redirect:/admin/municipalities";
    }

    @GetMapping("/admin/municipalities")
    public String municipalityDashboard(
        @RequestParam(name = "issueId", required = false) Long issueId,
        Model model
    ) {
        MunicipalityAdminDashboard dashboard = municipalityReviewService.getDashboard();
        MunicipalityIssueSummary selectedIssue = findSelectedIssue(dashboard, issueId);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("selectedIssue", selectedIssue);
        model.addAttribute("selectedIssueId", selectedIssue != null ? selectedIssue.id() : null);
        model.addAttribute("issueCorrectionForm", buildCorrectionForm(selectedIssue));
        return "admin/municipalities";
    }

    @PostMapping("/admin/municipalities/issues/{issueId}")
    public String correctMunicipalityIssue(
        @PathVariable Long issueId,
        @Valid @ModelAttribute("issueCorrectionForm") MunicipalityIssueCorrectionForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        MunicipalityAdminDashboard dashboard = municipalityReviewService.getDashboard();
        MunicipalityIssueSummary selectedIssue = findSelectedIssue(dashboard, issueId);

        if (bindingResult.hasErrors()) {
            model.addAttribute("dashboard", dashboard);
            model.addAttribute("selectedIssue", selectedIssue);
            model.addAttribute("selectedIssueId", issueId);
            model.addAttribute("editingIssueId", issueId);
            return "admin/municipalities";
        }

        try {
            municipalityReviewService.correctIssue(issueId, form.municipalityCode(), form.municipalityName(), form.resolutionNote());
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("issue.invalid", exception.getMessage());
            model.addAttribute("dashboard", dashboard);
            model.addAttribute("selectedIssue", selectedIssue);
            model.addAttribute("selectedIssueId", issueId);
            model.addAttribute("editingIssueId", issueId);
            return "admin/municipalities";
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Municipio corregido y regla aprendida.");
        return "redirect:/admin/municipalities";
    }

    private MunicipalityIssueSummary findSelectedIssue(MunicipalityAdminDashboard dashboard, Long issueId) {
        if (dashboard.openIssues().isEmpty()) {
            return null;
        }

        if (issueId != null) {
            return dashboard.openIssues().stream()
                .filter(issue -> issue.id().equals(issueId))
                .findFirst()
                .orElseGet(() -> firstIssue(dashboard));
        }

        return firstIssue(dashboard);
    }

    private MunicipalityIssueSummary firstIssue(MunicipalityAdminDashboard dashboard) {
        return dashboard.openIssues().stream()
            .min(Comparator.comparing(MunicipalityIssueSummary::createdAt))
            .orElse(null);
    }

    private MunicipalityIssueCorrectionForm buildCorrectionForm(MunicipalityIssueSummary selectedIssue) {
        if (selectedIssue == null) {
            return new MunicipalityIssueCorrectionForm();
        }
        return new MunicipalityIssueCorrectionForm(
            selectedIssue.assignedMunicipalityCode(),
            selectedIssue.assignedMunicipalityName(),
            selectedIssue.resolutionNote() != null ? selectedIssue.resolutionNote() : ""
        );
    }
}
