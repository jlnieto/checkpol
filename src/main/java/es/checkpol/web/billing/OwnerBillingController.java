package es.checkpol.web.billing;

import es.checkpol.service.billing.BillingAccountService;
import es.checkpol.service.billing.BillingConfigurationException;
import es.checkpol.service.billing.BillingProviderException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OwnerBillingController {

    private final BillingAccountService billingAccountService;

    public OwnerBillingController(BillingAccountService billingAccountService) {
        this.billingAccountService = billingAccountService;
    }

    @GetMapping("/bookings/billing")
    public String billing(Model model) {
        model.addAttribute("billingStatus", billingAccountService.getCurrentOwnerBillingStatus());
        model.addAttribute("invoices", billingAccountService.getCurrentOwnerInvoices());
        return "bookings/billing";
    }

    @PostMapping("/bookings/billing/portal")
    public String portal(RedirectAttributes redirectAttributes) {
        try {
            return "redirect:" + billingAccountService.createCurrentOwnerPortalSession();
        } catch (IllegalStateException | BillingConfigurationException | BillingProviderException ex) {
            redirectAttributes.addFlashAttribute("flashKind", "error");
            redirectAttributes.addFlashAttribute("flashMessage", ex.getMessage());
            return "redirect:/bookings/billing";
        }
    }
}
