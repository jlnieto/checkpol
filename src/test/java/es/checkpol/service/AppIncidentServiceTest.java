package es.checkpol.service;

import es.checkpol.domain.AppIncident;
import es.checkpol.domain.AppIncidentArea;
import es.checkpol.domain.AppIncidentSeverity;
import es.checkpol.domain.AppIncidentStatus;
import es.checkpol.repository.AppIncidentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppIncidentServiceTest {

    private final AppIncidentRepository appIncidentRepository = mock(AppIncidentRepository.class);
    private final AppIncidentService service = new AppIncidentService(appIncidentRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registersUnexpectedOwnerExceptionWithContext() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("joana", "ignored");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(appIncidentRepository.save(any(AppIncident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/bookings/15/guests/22/edit");
        request.setQueryString("step=identity");
        request.addHeader("User-Agent", "Mozilla");

        AppIncident incident = service.registerUnexpectedException(request, new IllegalStateException("No se pudo guardar"));

        assertEquals(AppIncidentArea.OWNER, incident.getArea());
        assertEquals(AppIncidentSeverity.ERROR, incident.getSeverity());
        assertEquals(AppIncidentStatus.OPEN, incident.getStatus());
        assertEquals("POST", incident.getMethod());
        assertEquals("/bookings/15/guests/22/edit?step=identity", incident.getPath());
        assertEquals("joana", incident.getUsername());
        assertEquals(15L, incident.getBookingId());
        assertEquals(22L, incident.getGuestId());
        assertNotNull(incident.getStackTrace());
        verify(appIncidentRepository).save(any(AppIncident.class));
    }

    @Test
    void returnsActiveSummary() {
        when(appIncidentRepository.countByStatus(AppIncidentStatus.OPEN)).thenReturn(2L);
        when(appIncidentRepository.countByStatusIn(List.of(AppIncidentStatus.OPEN, AppIncidentStatus.REVIEWED))).thenReturn(3L);

        AppIncidentService.IncidentDashboardSummary summary = service.getDashboardSummary();

        assertEquals(2L, summary.openCount());
        assertEquals(3L, summary.activeCount());
    }

    @Test
    void marksIncidentAsReviewedAndResolved() {
        AppIncident incident = incident();
        when(appIncidentRepository.findById(7L)).thenReturn(Optional.of(incident));

        service.markReviewed(7L);
        service.markResolved(7L);

        assertEquals(AppIncidentStatus.RESOLVED, incident.getStatus());
        assertNotNull(incident.getReviewedAt());
        assertNotNull(incident.getResolvedAt());
    }

    private AppIncident incident() {
        return new AppIncident(
            AppIncidentArea.ADMIN,
            AppIncidentSeverity.ERROR,
            "Error",
            "java.lang.RuntimeException: Error",
            "stack",
            "GET",
            "/admin",
            "admin",
            "Mozilla",
            null,
            null,
            null,
            null,
            OffsetDateTime.parse("2026-04-18T10:00:00Z")
        );
    }
}
