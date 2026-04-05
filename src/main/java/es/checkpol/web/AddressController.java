package es.checkpol.web;

import es.checkpol.service.AddressService;
import es.checkpol.service.BookingService;
import es.checkpol.service.GuestSelfServiceService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AddressController {

    private final AddressService addressService;
    private final BookingService bookingService;
    private final GuestSelfServiceService guestSelfServiceService;

    public AddressController(
        AddressService addressService,
        BookingService bookingService,
        GuestSelfServiceService guestSelfServiceService
    ) {
        this.addressService = addressService;
        this.bookingService = bookingService;
        this.guestSelfServiceService = guestSelfServiceService;
    }

    @GetMapping(value = "/bookings/{bookingId}/addresses", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public java.util.List<AddressSummary> listBookingAddresses(@PathVariable Long bookingId) {
        return addressService.findByBookingId(bookingId).stream()
            .map(address -> new AddressSummary(
                address.getId(),
                address.getDisplayLine1(),
                address.getDisplayLine2()
            ))
            .toList();
    }

    @GetMapping("/bookings/{bookingId}/addresses/new")
    public String newBookingAddress(
        @PathVariable Long bookingId,
        @RequestParam(name = "guestId", required = false) Long guestId,
        Model model
    ) {
        return populateBookingAddressForm(model, bookingId, new AddressForm(), guestId);
    }

    @PostMapping("/bookings/{bookingId}/addresses")
    public String createBookingAddress(
        @PathVariable Long bookingId,
        @RequestParam(name = "guestId", required = false) Long guestId,
        @Valid @ModelAttribute("addressForm") AddressForm form,
        BindingResult bindingResult,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            return populateBookingAddressForm(model, bookingId, form, guestId);
        }

        try {
            Long addressId = addressService.create(bookingId, form).getId();
            return redirectToBookingGuestForm(bookingId, guestId, addressId);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("address.invalid", exception.getMessage());
            return populateBookingAddressForm(model, bookingId, form, guestId);
        }
    }

    @GetMapping("/guest-access/{token}/addresses/new")
    public String newPublicAddress(
        @PathVariable String token,
        @RequestParam(name = "guestId", required = false) Long guestId,
        @RequestParam(name = "slot", required = false) Integer slot,
        @RequestParam(name = "extra", defaultValue = "false") boolean extra,
        Model model
    ) {
        return populatePublicAddressForm(model, token, new AddressForm(), guestId, slot, extra);
    }

    @PostMapping("/guest-access/{token}/addresses")
    public String createPublicAddress(
        @PathVariable String token,
        @RequestParam(name = "guestId", required = false) Long guestId,
        @RequestParam(name = "slot", required = false) Integer slot,
        @RequestParam(name = "extra", defaultValue = "false") boolean extra,
        @Valid @ModelAttribute("addressForm") AddressForm form,
        BindingResult bindingResult,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            return populatePublicAddressForm(model, token, form, guestId, slot, extra);
        }

        try {
            Long addressId = guestSelfServiceService.createAddress(token, form);
            return redirectToPublicGuestForm(token, guestId, slot, extra, addressId);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("address.invalid", exception.getMessage());
            return populatePublicAddressForm(model, token, form, guestId, slot, extra);
        }
    }

    private String populateBookingAddressForm(Model model, Long bookingId, AddressForm form, Long guestId) {
        var details = bookingService.getDetails(bookingId);
        model.addAttribute("details", details);
        model.addAttribute("addressForm", form);
        model.addAttribute("countries", GuestFormOptions.countries());
        model.addAttribute("formAction", "/bookings/" + bookingId + "/addresses");
        model.addAttribute("guestId", guestId);
        model.addAttribute("pageTitle", "Nueva dirección");
        model.addAttribute("backUrl", guestId != null
            ? "/bookings/" + bookingId + "/guests/" + guestId + "/edit?step=3"
            : "/bookings/" + bookingId + "/guests/new?step=3");
        return "addresses/form";
    }

    private String populatePublicAddressForm(Model model, String token, AddressForm form, Long guestId, Integer slot, boolean extra) {
        var access = guestSelfServiceService.getByToken(token);
        model.addAttribute("access", access);
        model.addAttribute("addressForm", form);
        model.addAttribute("countries", GuestFormOptions.countries());
        model.addAttribute("formAction", "/guest-access/" + token + "/addresses");
        model.addAttribute("guestId", guestId);
        model.addAttribute("slot", slot);
        model.addAttribute("extra", extra);
        model.addAttribute("pageTitle", "Nueva dirección");
        model.addAttribute("backUrl", redirectToPublicGuestForm(token, guestId, slot, extra, null).replace("redirect:", ""));
        return "public/address-form";
    }

    private String redirectToBookingGuestForm(Long bookingId, Long guestId, Long addressId) {
        if (guestId != null) {
            return "redirect:/bookings/" + bookingId + "/guests/" + guestId + "/edit?step=3&selectedAddressId=" + addressId;
        }
        return "redirect:/bookings/" + bookingId + "/guests/new?step=3&selectedAddressId=" + addressId;
    }

    private String redirectToPublicGuestForm(String token, Long guestId, Integer slot, boolean extra, Long addressId) {
        if (guestId != null) {
            String suffix = addressId == null ? "" : "&selectedAddressId=" + addressId;
            return "redirect:/guest-access/" + token + "/guests/" + guestId + "/edit?step=3" + suffix;
        }

        StringBuilder url = new StringBuilder("/guest-access/" + token + "/guests/new?step=3");
        if (addressId != null) {
            url.append("&selectedAddressId=").append(addressId);
        }
        if (slot != null) {
            url.append("&slot=").append(slot);
        }
        if (extra) {
            url.append("&extra=true");
        }
        return "redirect:" + url;
    }

    public record AddressSummary(Long id, String line1, String line2) {
    }
}
