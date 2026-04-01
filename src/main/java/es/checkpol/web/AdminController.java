package es.checkpol.web;

import es.checkpol.domain.AppUserRole;
import es.checkpol.service.AdminUserSummary;
import es.checkpol.service.AppUserAdminService;
import es.checkpol.service.MunicipalityAdminService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AdminController {

    private final AppUserAdminService appUserAdminService;
    private final MunicipalityAdminService municipalityAdminService;

    public AdminController(AppUserAdminService appUserAdminService, MunicipalityAdminService municipalityAdminService) {
        this.appUserAdminService = appUserAdminService;
        this.municipalityAdminService = municipalityAdminService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        List<AdminUserSummary> users = appUserAdminService.findAllUsers();
        MunicipalityAdminService.DashboardSummary dashboard = municipalityAdminService.getDashboardSummary();
        populateUserMetrics(model, users);
        model.addAttribute("users", users);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("sourceHealth", dashboard.sourceHealth());
        return "admin/index";
    }

    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        List<AdminUserSummary> users = appUserAdminService.findAllUsers();
        populateUserMetrics(model, users);
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/admin/users/new")
    public String newOwner(Model model) {
        return populateForm(model, new AdminUserForm(), "/admin/users", "Nuevo usuario", "Crear usuario");
    }

    @PostMapping("/admin/users")
    public String createOwner(
        @Valid @ModelAttribute("adminUserForm") AdminUserForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateForm(model, form, "/admin/users", "Nuevo usuario", "Crear usuario");
        }

        try {
            appUserAdminService.createOwner(form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("user.invalid", exception.getMessage());
            return populateForm(model, form, "/admin/users", "Nuevo usuario", "Crear usuario");
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Usuario creado correctamente.");
        redirectAttributes.addFlashAttribute("flashKind", "success");
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/{userId}/edit")
    public String editOwner(@PathVariable Long userId, Model model) {
        return populateForm(model, appUserAdminService.getOwnerForm(userId), "/admin/users/" + userId, "Editar usuario", "Guardar cambios");
    }

    @PostMapping("/admin/users/{userId}")
    public String updateOwner(
        @PathVariable Long userId,
        @Valid @ModelAttribute("adminUserForm") AdminUserForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return populateForm(model, form, "/admin/users/" + userId, "Editar usuario", "Guardar cambios");
        }

        try {
            appUserAdminService.updateOwner(userId, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("user.invalid", exception.getMessage());
            return populateForm(model, form, "/admin/users/" + userId, "Editar usuario", "Guardar cambios");
        }

        redirectAttributes.addFlashAttribute("flashMessage", "Usuario actualizado correctamente.");
        redirectAttributes.addFlashAttribute("flashKind", "success");
        return "redirect:/admin/users";
    }

    private String populateForm(Model model, AdminUserForm form, String action, String title, String submitLabel) {
        model.addAttribute("adminUserForm", form);
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
        model.addAttribute("submitLabel", submitLabel);
        return "admin/user-form";
    }

    private void populateUserMetrics(Model model, List<AdminUserSummary> users) {
        long activeUserCount = users.stream().filter(AdminUserSummary::active).count();
        long ownerCount = users.stream().filter(user -> user.role() == AppUserRole.OWNER).count();
        long activeOwnerCount = users.stream().filter(user -> user.role() == AppUserRole.OWNER && user.active()).count();
        model.addAttribute("userCount", users.size());
        model.addAttribute("ownerCount", ownerCount);
        model.addAttribute("activeUserCount", activeUserCount);
        model.addAttribute("inactiveUserCount", users.size() - activeUserCount);
        model.addAttribute("activeOwnerCount", activeOwnerCount);
        model.addAttribute("inactiveOwnerCount", ownerCount - activeOwnerCount);
    }
}
