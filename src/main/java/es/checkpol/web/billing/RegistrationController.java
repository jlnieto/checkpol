package es.checkpol.web.billing;

import es.checkpol.domain.billing.PendingSignup;
import es.checkpol.service.billing.BillingConfigurationException;
import es.checkpol.service.billing.BillingProviderException;
import es.checkpol.service.billing.RegistrationBillingService;
import es.checkpol.service.billing.RegistrationCheckout;
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
public class RegistrationController {

    private final RegistrationBillingService registrationBillingService;

    public RegistrationController(RegistrationBillingService registrationBillingService) {
        this.registrationBillingService = registrationBillingService;
    }

    @GetMapping("/registro")
    public String registrationForm(Model model) {
        if (!model.containsAttribute("registrationForm")) {
            model.addAttribute("registrationForm", new RegistrationForm());
        }
        return "public/register";
    }

    @PostMapping("/registro")
    public String startRegistration(
        @Valid @ModelAttribute("registrationForm") RegistrationForm form,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "public/register";
        }

        try {
            RegistrationCheckout checkout = registrationBillingService.startSignup(form);
            return "redirect:/registro/pago/" + checkout.signup().getToken();
        } catch (IllegalArgumentException | BillingConfigurationException | BillingProviderException ex) {
            bindingResult.reject("registration", ex.getMessage());
            return "public/register";
        }
    }

    @GetMapping("/registro/pago/{token}")
    public String payment(@PathVariable String token, Model model) {
        PendingSignup signup = registrationBillingService.getSignupByToken(token);
        if (!signup.isPendingPayment()) {
            return "redirect:/registro/confirmando/" + token;
        }
        model.addAttribute("signup", signup);
        model.addAttribute("clientSecret", signup.getCheckoutClientSecret());
        model.addAttribute("publishableKey", registrationBillingService.getPublishableKey());
        return "public/register-payment";
    }

    @GetMapping("/registro/confirmando/{token}")
    public String confirming(@PathVariable String token, Model model) {
        PendingSignup signup = registrationBillingService.getSignupByToken(token);
        model.addAttribute("signup", signup);
        if (!signup.isPendingPayment()) {
            return "public/register-completed";
        }
        return "public/register-confirming";
    }
}
