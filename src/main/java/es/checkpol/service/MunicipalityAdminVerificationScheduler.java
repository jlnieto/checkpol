package es.checkpol.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class MunicipalityAdminVerificationScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MunicipalityAdminVerificationScheduler.class);

    private final MunicipalityAdminService municipalityAdminService;
    private final AdminSettingsService adminSettingsService;
    private final Clock clock;

    private volatile ZonedDateTime lastTriggeredAt;

    @Autowired
    public MunicipalityAdminVerificationScheduler(
        MunicipalityAdminService municipalityAdminService,
        AdminSettingsService adminSettingsService
    ) {
        this(municipalityAdminService, adminSettingsService, Clock.systemUTC());
    }

    MunicipalityAdminVerificationScheduler(
        MunicipalityAdminService municipalityAdminService,
        AdminSettingsService adminSettingsService,
        Clock clock
    ) {
        this.municipalityAdminService = municipalityAdminService;
        this.adminSettingsService = adminSettingsService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${checkpol.municipality.admin.verification.poll-delay-ms:30000}")
    public void verifyOfficialSources() {
        AdminSettingsService.VerificationSettings settings = adminSettingsService.getVerificationSettings();
        if (!settings.enabled()) {
            return;
        }

        try {
            ZoneId zoneId = ZoneId.of(settings.zone());
            CronExpression cron = CronExpression.parse(settings.cron());
            ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(zoneId).withNano(0);
            ZonedDateTime windowStart = now.minusMinutes(1);
            ZonedDateTime nextScheduled = cron.next(windowStart);

            if (nextScheduled == null || nextScheduled.isAfter(now)) {
                return;
            }
            if (lastTriggeredAt != null && !nextScheduled.isAfter(lastTriggeredAt)) {
                return;
            }

            MunicipalityAdminService.VerificationSummary summary = municipalityAdminService.verifyDefaultSources(settings.triggeredByUsername());
            lastTriggeredAt = nextScheduled;

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
