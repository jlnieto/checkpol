package es.checkpol.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MunicipalityAdminVerificationSchedulerTest {

    @Test
    void skipsVerificationWhenDisabled() {
        MunicipalityAdminService municipalityAdminService = mock(MunicipalityAdminService.class);
        MunicipalityAdminVerificationScheduler scheduler = new MunicipalityAdminVerificationScheduler(
            municipalityAdminService,
            false,
            "system-verifier"
        );

        scheduler.verifyOfficialSources();

        verify(municipalityAdminService, never()).verifyDefaultSources("system-verifier");
    }

    @Test
    void triggersVerificationWhenEnabled() {
        MunicipalityAdminService municipalityAdminService = mock(MunicipalityAdminService.class);
        when(municipalityAdminService.verifyDefaultSources("system-verifier")).thenReturn(
            new MunicipalityAdminService.VerificationSummary("ok", "Fuentes oficiales verificadas.", 8100, 23000, List.of())
        );
        MunicipalityAdminVerificationScheduler scheduler = new MunicipalityAdminVerificationScheduler(
            municipalityAdminService,
            true,
            "system-verifier"
        );

        scheduler.verifyOfficialSources();

        verify(municipalityAdminService).verifyDefaultSources("system-verifier");
    }
}
