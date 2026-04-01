package es.checkpol.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MunicipalityAdminVerificationScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MunicipalityAdminVerificationScheduler.class);

    private final MunicipalityAdminService municipalityAdminService;
    private final boolean enabled;
    private final String triggeredByUsername;

    public MunicipalityAdminVerificationScheduler(
        MunicipalityAdminService municipalityAdminService,
        @Value("${checkpol.municipality.admin.verification.enabled:false}") boolean enabled,
        @Value("${checkpol.municipality.admin.verification.triggered-by:system-verifier}") String triggeredByUsername
    ) {
        this.municipalityAdminService = municipalityAdminService;
        this.enabled = enabled;
        this.triggeredByUsername = triggeredByUsername;
    }

    @Scheduled(
        cron = "${checkpol.municipality.admin.verification.cron:0 0 6 * * *}",
        zone = "${checkpol.municipality.admin.verification.zone:Europe/Madrid}"
    )
    public void verifyOfficialSources() {
        if (!enabled) {
            return;
        }

        try {
            MunicipalityAdminService.VerificationSummary summary = municipalityAdminService.verifyDefaultSources(triggeredByUsername);
            if (!summary.warnings().isEmpty()) {
                LOGGER.warn("Municipality source verification completed with warnings: {}", summary.warnings());
            } else {
                LOGGER.info("Municipality source verification completed successfully.");
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Municipality source verification failed.", exception);
        }
    }
}
