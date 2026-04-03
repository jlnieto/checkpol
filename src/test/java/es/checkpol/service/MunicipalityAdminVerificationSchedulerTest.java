package es.checkpol.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MunicipalityAdminVerificationSchedulerTest {

    @Test
    void skipsVerificationWhenDisabled() {
        MunicipalityAdminService municipalityAdminService = mock(MunicipalityAdminService.class);
        AdminSettingsService adminSettingsService = mock(AdminSettingsService.class);
        when(adminSettingsService.getVerificationSettings()).thenReturn(new AdminSettingsService.VerificationSettings(
            false,
            "0 0 6 * * *",
            "Europe/Madrid",
            "system-verifier",
            null,
            null
        ));
        MunicipalityAdminVerificationScheduler scheduler = new MunicipalityAdminVerificationScheduler(
            municipalityAdminService,
            adminSettingsService,
            fixedClock("2026-04-03T09:00:05Z")
        );

        scheduler.verifyOfficialSources();

        verify(municipalityAdminService, never()).verifyDefaultSources("system-verifier");
    }

    @Test
    void triggersVerificationWhenCurrentTimeMatchesCron() {
        MunicipalityAdminService municipalityAdminService = mock(MunicipalityAdminService.class);
        AdminSettingsService adminSettingsService = mock(AdminSettingsService.class);
        when(adminSettingsService.getVerificationSettings()).thenReturn(new AdminSettingsService.VerificationSettings(
            true,
            "0 0 6 * * *",
            "Europe/Madrid",
            "system-verifier",
            null,
            null
        ));
        when(municipalityAdminService.verifyDefaultSources("system-verifier")).thenReturn(
            new MunicipalityAdminService.VerificationSummary("ok", "Fuentes oficiales verificadas.", 8100, 23000, List.of())
        );
        MunicipalityAdminVerificationScheduler scheduler = new MunicipalityAdminVerificationScheduler(
            municipalityAdminService,
            adminSettingsService,
            fixedClock("2026-04-03T04:00:20Z")
        );

        scheduler.verifyOfficialSources();
        scheduler.verifyOfficialSources();

        verify(municipalityAdminService, times(1)).verifyDefaultSources("system-verifier");
    }

    @Test
    void skipsVerificationWhenCronDoesNotMatchWindow() {
        MunicipalityAdminService municipalityAdminService = mock(MunicipalityAdminService.class);
        AdminSettingsService adminSettingsService = mock(AdminSettingsService.class);
        when(adminSettingsService.getVerificationSettings()).thenReturn(new AdminSettingsService.VerificationSettings(
            true,
            "0 0 6 * * *",
            "Europe/Madrid",
            "system-verifier",
            null,
            null
        ));
        MunicipalityAdminVerificationScheduler scheduler = new MunicipalityAdminVerificationScheduler(
            municipalityAdminService,
            adminSettingsService,
            fixedClock("2026-04-03T05:17:20Z")
        );

        scheduler.verifyOfficialSources();

        verify(municipalityAdminService, never()).verifyDefaultSources("system-verifier");
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }
}
