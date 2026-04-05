package es.checkpol.service;

import es.checkpol.domain.AppUser;
import es.checkpol.web.OwnerSesSettingsForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnerSesSettingsService {

    private final CurrentAppUserService currentAppUserService;
    private final AppUserSesService appUserSesService;

    public OwnerSesSettingsService(
        CurrentAppUserService currentAppUserService,
        AppUserSesService appUserSesService
    ) {
        this.currentAppUserService = currentAppUserService;
        this.appUserSesService = appUserSesService;
    }

    @Transactional(readOnly = true)
    public OwnerSesSettingsForm getCurrentForm() {
        AppUser owner = currentAppUserService.requireCurrentUserEntity();
        return new OwnerSesSettingsForm(
            owner.getSesArrendadorCode() == null ? "" : owner.getSesArrendadorCode(),
            owner.getSesWsUsername() == null ? "" : owner.getSesWsUsername(),
            ""
        );
    }

    @Transactional(readOnly = true)
    public AppUser getCurrentOwner() {
        return currentAppUserService.requireCurrentUserEntity();
    }

    @Transactional
    public AppUser save(OwnerSesSettingsForm form) {
        AppUser owner = currentAppUserService.requireCurrentUserEntity();
        appUserSesService.updateSesConfiguration(owner, form.sesArrendadorCode(), form.sesWsUsername(), form.sesWsPassword());
        return owner;
    }

    @Transactional
    public SesConnectionTestResult saveAndTest(OwnerSesSettingsForm form) {
        AppUser owner = save(form);
        return appUserSesService.testAndStore(owner);
    }
}
