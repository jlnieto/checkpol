package es.checkpol.web;

import es.checkpol.domain.DocumentType;
import es.checkpol.domain.GuestRelationship;
import es.checkpol.domain.GuestSex;
import es.checkpol.domain.Guest;
import es.checkpol.service.GuestSelfServiceService;
import jakarta.servlet.http.HttpSession;
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

import java.util.ArrayList;
import java.util.List;

@Controller
public class GuestSelfServiceController {

    private final GuestSelfServiceService guestSelfServiceService;
    private final GuestWizardDraftStore guestWizardDraftStore;

    public GuestSelfServiceController(
        GuestSelfServiceService guestSelfServiceService,
        GuestWizardDraftStore guestWizardDraftStore
    ) {
        this.guestSelfServiceService = guestSelfServiceService;
        this.guestWizardDraftStore = guestWizardDraftStore;
    }

    @GetMapping("/guest-access/{token}")
    public String showDashboard(@PathVariable String token, Model model) {
        var access = guestSelfServiceService.getByToken(token);
        long completedGuestCount = access.guests().stream().filter(Guest::hasMinimumDataForTravelerPart).count();
        model.addAttribute("access", access);
        model.addAttribute("guestCards", buildGuestCards(access));
        model.addAttribute("completedGuestCount", completedGuestCount);
        model.addAttribute("allGuestsCompleted", access.expectedGuestCount() > 0 && completedGuestCount >= access.expectedGuestCount());
        return "public/guest-access";
    }

    @GetMapping("/guest-access/{token}/complete")
    public String showCompleted(@PathVariable String token, Model model) {
        var access = guestSelfServiceService.getByToken(token);
        long completedGuestCount = access.guests().stream().filter(Guest::hasMinimumDataForTravelerPart).count();
        if (access.expectedGuestCount() <= 0 || completedGuestCount < access.expectedGuestCount()) {
            return "redirect:/guest-access/" + token;
        }
        model.addAttribute("access", access);
        return "public/guest-complete";
    }

    @GetMapping("/guest-access/{token}/guests/new")
    public String newGuest(
        @PathVariable String token,
        @RequestParam(name = "slot", required = false) Integer slot,
        @RequestParam(name = "extra", defaultValue = "false") boolean extra,
        @RequestParam(name = "selectedAddressId", required = false) Long selectedAddressId,
        @RequestParam(name = "step", required = false) Integer step,
        HttpSession session,
        Model model
    ) {
        boolean resumingDraft = step != null || selectedAddressId != null;
        if (!resumingDraft) {
            guestWizardDraftStore.clearPublicDraft(session, token, null, slot, extra);
        }
        GuestForm form = guestWizardDraftStore.getPublicDraft(session, token, null, slot, extra)
            .filter(draft -> resumingDraft)
            .map(draft -> selectedAddressId == null ? draft : draft.withAddressId(selectedAddressId))
            .orElseGet(() -> selectedAddressId == null ? new GuestForm() : new GuestForm().withAddressId(selectedAddressId));
        populateWizard(model, token, form, wizardTitle(slot, extra), wizardHelp(slot, extra), null, slot, extra, step);
        return "public/guest-form";
    }

    @PostMapping({"/guest-access/{token}", "/guest-access/{token}/guests"})
    public String submit(
        @PathVariable String token,
        @RequestParam(name = "slot", required = false) Integer slot,
        @RequestParam(name = "extra", defaultValue = "false") boolean extra,
        @Valid @ModelAttribute("guestForm") GuestForm form,
        BindingResult bindingResult,
        HttpSession session,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            populateWizard(model, token, form, wizardTitle(slot, extra), wizardHelp(slot, extra), null, slot, extra, 3);
            return "public/guest-form";
        }

        try {
            guestSelfServiceService.submitGuest(token, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("guest.invalid", exception.getMessage());
            populateWizard(model, token, form, wizardTitle(slot, extra), wizardHelp(slot, extra), null, slot, extra, 3);
            return "public/guest-form";
        }

        guestWizardDraftStore.clearPublicDraft(session, token, null, slot, extra);
        redirectAttributes.addFlashAttribute("flashMessage", "Tus datos se han enviado correctamente.");
        return "redirect:/guest-access/" + token;
    }

    @PostMapping("/guest-access/{token}/guests/draft-address")
    public String saveNewGuestDraftBeforeAddress(
        @PathVariable String token,
        @RequestParam(name = "slot", required = false) Integer slot,
        @RequestParam(name = "extra", defaultValue = "false") boolean extra,
        @ModelAttribute("guestForm") GuestForm form,
        HttpSession session
    ) {
        guestWizardDraftStore.savePublicDraft(session, token, null, slot, extra, form);

        StringBuilder url = new StringBuilder("/guest-access/").append(token).append("/addresses/new");
        boolean hasQuery = false;
        if (slot != null) {
            url.append("?slot=").append(slot);
            hasQuery = true;
        }
        if (extra) {
            url.append(hasQuery ? "&" : "?").append("extra=true");
        }
        return "redirect:" + url;
    }

    @GetMapping("/guest-access/{token}/guests/{guestId}/edit")
    public String edit(
        @PathVariable String token,
        @PathVariable Long guestId,
        @RequestParam(name = "selectedAddressId", required = false) Long selectedAddressId,
        @RequestParam(name = "step", required = false) Integer step,
        HttpSession session,
        Model model
    ) {
        var access = guestSelfServiceService.getByToken(token);
        boolean resumingDraft = step != null || selectedAddressId != null;
        if (!resumingDraft) {
            guestWizardDraftStore.clearPublicDraft(session, token, guestId, null, false);
        }
        GuestForm form = guestWizardDraftStore.getPublicDraft(session, token, guestId, null, false)
            .filter(draft -> resumingDraft)
            .orElseGet(() -> guestSelfServiceService.getGuestForm(token, guestId));
        if (selectedAddressId != null) {
            form = form.withAddressId(selectedAddressId);
        }
        populateWizard(
            model,
            token,
            form,
            wizardTitle(slotNumber(access.guests(), guestId), false),
            "Introduce los datos tal como aparecen en el documento",
            guestId,
            slotNumber(access.guests(), guestId),
            false,
            step
        );
        model.addAttribute("publicEditGuestId", guestId);
        return "public/guest-form";
    }

    @PostMapping("/guest-access/{token}/guests/{guestId}")
    public String update(
        @PathVariable String token,
        @PathVariable Long guestId,
        @Valid @ModelAttribute("guestForm") GuestForm form,
        BindingResult bindingResult,
        HttpSession session,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        var access = guestSelfServiceService.getByToken(token);
        if (bindingResult.hasErrors()) {
            populateWizard(
                model,
                token,
                form,
                wizardTitle(slotNumber(access.guests(), guestId), false),
                "Introduce los datos tal como aparecen en el documento",
                guestId,
                slotNumber(access.guests(), guestId),
                false,
                3
            );
            model.addAttribute("publicEditGuestId", guestId);
            return "public/guest-form";
        }

        try {
            guestSelfServiceService.updateGuest(token, guestId, form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("guest.invalid", exception.getMessage());
            populateWizard(
                model,
                token,
                form,
                wizardTitle(slotNumber(access.guests(), guestId), false),
                "Introduce los datos tal como aparecen en el documento",
                guestId,
                slotNumber(access.guests(), guestId),
                false,
                3
            );
            model.addAttribute("publicEditGuestId", guestId);
            return "public/guest-form";
        }

        guestWizardDraftStore.clearPublicDraft(session, token, guestId, null, false);
        redirectAttributes.addFlashAttribute("flashMessage", "Tus datos se han actualizado correctamente.");
        return "redirect:/guest-access/" + token;
    }

    @PostMapping("/guest-access/{token}/guests/{guestId}/draft-address")
    public String saveExistingGuestDraftBeforeAddress(
        @PathVariable String token,
        @PathVariable Long guestId,
        @ModelAttribute("guestForm") GuestForm form,
        HttpSession session
    ) {
        guestWizardDraftStore.savePublicDraft(session, token, guestId, null, false, form);
        return "redirect:/guest-access/" + token + "/addresses/new?guestId=" + guestId;
    }

    private void populateWizard(
        Model model,
        String token,
        GuestForm form,
        String title,
        String help,
        Long guestId,
        Integer slot,
        boolean extra,
        Integer step
    ) {
        var access = guestSelfServiceService.getByToken(token);
        model.addAttribute("access", access);
        model.addAttribute("guestForm", form);
        model.addAttribute("documentTypes", DocumentType.values());
        model.addAttribute("countries", GuestFormOptions.countries());
        model.addAttribute("relationships", GuestRelationship.values());
        model.addAttribute("sexes", GuestSex.values());
        model.addAttribute("addresses", access.addresses());
        model.addAttribute("guestWizardTitle", title);
        model.addAttribute("guestWizardHelp", help);
        model.addAttribute("initialStep", step == null ? null : Math.max(step - 1, 0));
        StringBuilder newAddressUrl = new StringBuilder("/guest-access/" + token + "/addresses/new");
        boolean hasQuery = false;
        if (guestId != null) {
            newAddressUrl.append("?guestId=").append(guestId);
            hasQuery = true;
        } else if (slot != null) {
            newAddressUrl.append("?slot=").append(slot);
            hasQuery = true;
        }
        if (extra) {
            newAddressUrl.append(hasQuery ? "&" : "?").append("extra=true");
        }
        model.addAttribute("newAddressUrl", newAddressUrl.toString());
        String saveDraftAction = guestId == null
            ? "/guest-access/" + token + "/guests/draft-address"
            : "/guest-access/" + token + "/guests/" + guestId + "/draft-address";
        model.addAttribute("saveAddressDraftAction", saveDraftAction);
    }

    private List<PublicGuestCard> buildGuestCards(es.checkpol.service.GuestSelfServiceDetails access) {
        List<PublicGuestCard> cards = new ArrayList<>();
        int expectedGuests = Math.max(access.expectedGuestCount() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) access.expectedGuestCount(), access.guests().size());

        for (int index = 0; index < expectedGuests; index++) {
            int guestNumber = index + 1;
            if (index < access.guests().size()) {
                Guest guest = access.guests().get(index);
                boolean completed = guest.hasMinimumDataForTravelerPart();
                cards.add(new PublicGuestCard(
                    guest.getDisplayName(),
                    "",
                    completed ? "Editar datos" : "Completar datos",
                    "/guest-access/" + access.booking().getSelfServiceToken() + "/guests/" + guest.getId() + "/edit",
                    completed ? "Completado" : "Faltan datos",
                    completed ? "status-ready" : "status-pending",
                    false
                ));
                continue;
            }

            cards.add(new PublicGuestCard(
                "Huésped " + guestNumber,
                "",
                "Completar datos",
                "/guest-access/" + access.booking().getSelfServiceToken() + "/guests/new?slot=" + guestNumber,
                "Faltan datos",
                "status-pending",
                false
            ));
        }

        cards.add(new PublicGuestCard(
            "Añadir otro huésped",
            "Solo si sois más personas",
            "Completar datos",
            "/guest-access/" + access.booking().getSelfServiceToken() + "/guests/new?extra=true",
            "",
            "",
            true
        ));

        return cards;
    }

    private int slotNumber(List<Guest> guests, Long guestId) {
        for (int index = 0; index < guests.size(); index++) {
            if (guestId.equals(guests.get(index).getId())) {
                return index + 1;
            }
        }
        return guests.size() + 1;
    }

    private String wizardTitle(Integer slot, boolean extra) {
        if (slot != null && slot > 0) {
            return "Datos del huesped " + slot;
        }
        return extra ? "Datos de la persona adicional" : "Datos del huesped";
    }

    private String wizardHelp(Integer slot, boolean extra) {
        if (slot != null && slot > 0) {
            return "Introduce los datos tal como aparecen en el documento";
        }
        return extra
            ? "Usa esta ficha solo si finalmente se aloja una persona mas de las previstas en la reserva."
            : "Introduce los datos tal como aparecen en el documento";
    }
}
