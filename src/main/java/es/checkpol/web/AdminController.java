package es.checkpol.web;

import es.checkpol.service.AppUserAdminService;
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
public class AdminController {

    private final AppUserAdminService appUserAdminService;

    public AdminController(AppUserAdminService appUserAdminService) {
        this.appUserAdminService = appUserAdminService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("ownerCount", appUserAdminService.countOwners());
        model.addAttribute("users", appUserAdminService.findAllUsers());
        return "admin/index";
    }

    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        model.addAttribute("users", appUserAdminService.findAllUsers());
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
}
