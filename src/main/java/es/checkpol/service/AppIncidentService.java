package es.checkpol.service;

import es.checkpol.domain.AppIncident;
import es.checkpol.domain.AppIncidentArea;
import es.checkpol.domain.AppIncidentSeverity;
import es.checkpol.domain.AppIncidentStatus;
import es.checkpol.repository.AppIncidentRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AppIncidentService {

    private static final int MESSAGE_LIMIT = 300;
    private static final int DETAIL_LIMIT = 2000;
    private static final int STACK_LIMIT = 12000;
    private static final int PATH_LIMIT = 500;
    private static final int USER_AGENT_LIMIT = 500;
    private static final Pattern BOOKING_ID_PATTERN = Pattern.compile("/bookings/(\\d+)");
    private static final Pattern GUEST_ID_PATTERN = Pattern.compile("/guests/(\\d+)");
    private static final Pattern COMMUNICATION_ID_PATTERN = Pattern.compile("/communications/(\\d+)");
    private static final List<AppIncidentStatus> ACTIVE_STATUSES = List.of(AppIncidentStatus.OPEN, AppIncidentStatus.REVIEWED);

    private final AppIncidentRepository appIncidentRepository;

    public AppIncidentService(AppIncidentRepository appIncidentRepository) {
        this.appIncidentRepository = appIncidentRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppIncident registerUnexpectedException(HttpServletRequest request, Exception exception) {
        String path = path(request);
        AppIncident incident = new AppIncident(
            resolveArea(path),
            AppIncidentSeverity.ERROR,
            truncate(buildMessage(exception), MESSAGE_LIMIT),
            truncate(buildTechnicalDetail(exception), DETAIL_LIMIT),
            truncate(stackTrace(exception), STACK_LIMIT),
            truncate(request.getMethod(), 12),
            truncate(path, PATH_LIMIT),
            truncate(currentUsername(), 120),
            truncate(request.getHeader("User-Agent"), USER_AGENT_LIMIT),
            null,
            extractLong(BOOKING_ID_PATTERN, path),
            extractLong(GUEST_ID_PATTERN, path),
            extractLong(COMMUNICATION_ID_PATTERN, path),
            OffsetDateTime.now()
        );
        return appIncidentRepository.save(incident);
    }

    @Transactional(readOnly = true)
    public IncidentDashboardSummary getDashboardSummary() {
        return new IncidentDashboardSummary(
            appIncidentRepository.countByStatus(AppIncidentStatus.OPEN),
            appIncidentRepository.countByStatusIn(ACTIVE_STATUSES)
        );
    }

    @Transactional(readOnly = true)
    public List<IncidentRow> findRecentIncidents(String statusFilter) {
        List<AppIncident> incidents = loadIncidents(statusFilter);
        return incidents.stream()
            .map(this::toRow)
            .toList();
    }

    @Transactional
    public void markReviewed(Long incidentId) {
        AppIncident incident = appIncidentRepository.findById(incidentId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado la incidencia indicada."));
        incident.markReviewed(OffsetDateTime.now());
    }

    @Transactional
    public void markResolved(Long incidentId) {
        AppIncident incident = appIncidentRepository.findById(incidentId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado la incidencia indicada."));
        incident.markResolved(OffsetDateTime.now());
    }

    private List<AppIncident> loadIncidents(String statusFilter) {
        AppIncidentStatus requestedStatus = parseStatus(statusFilter);
        if (requestedStatus != null) {
            return appIncidentRepository.findTop100ByStatusOrderByCreatedAtDesc(requestedStatus);
        }
        if ("ACTIVE".equalsIgnoreCase(statusFilter)) {
            return appIncidentRepository.findTop100ByStatusInOrderByCreatedAtDesc(ACTIVE_STATUSES);
        }
        return appIncidentRepository.findTop100ByOrderByCreatedAtDesc();
    }

    private IncidentRow toRow(AppIncident incident) {
        return new IncidentRow(
            incident.getId(),
            incident.getCode(),
            incident.getArea(),
            incident.getSeverity(),
            incident.getStatus(),
            incident.getMessage(),
            incident.getTechnicalDetail(),
            incident.getStackTrace(),
            incident.getMethod(),
            incident.getPath(),
            incident.getUsername(),
            incident.getUserAgent(),
            incident.getBookingId(),
            incident.getGuestId(),
            incident.getCommunicationId(),
            incident.getCreatedAt(),
            incident.getReviewedAt(),
            incident.getResolvedAt()
        );
    }

    private AppIncidentStatus parseStatus(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter) || "ACTIVE".equalsIgnoreCase(statusFilter)) {
            return null;
        }
        try {
            return AppIncidentStatus.valueOf(statusFilter.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private AppIncidentArea resolveArea(String path) {
        if (path.startsWith("/admin")) {
            return AppIncidentArea.ADMIN;
        }
        if (path.startsWith("/guest-access")) {
            return AppIncidentArea.PUBLIC_GUEST;
        }
        if (path.startsWith("/bookings") || path.startsWith("/accommodations") || path.startsWith("/addresses") || path.startsWith("/guests")) {
            return AppIncidentArea.OWNER;
        }
        return AppIncidentArea.SYSTEM;
    }

    private String path(HttpServletRequest request) {
        String uri = request.getRequestURI() == null ? "/" : request.getRequestURI();
        String query = request.getQueryString();
        return query == null || query.isBlank() ? uri : uri + "?" + query;
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        return "anonymousUser".equals(name) ? null : name;
    }

    private Long extractLong(Pattern pattern, String path) {
        Matcher matcher = pattern.matcher(path);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String buildMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    private String buildTechnicalDetail(Exception exception) {
        Throwable root = rootCause(exception);
        String rootMessage = root.getMessage();
        if (rootMessage == null || rootMessage.isBlank()) {
            return root.getClass().getName();
        }
        return root.getClass().getName() + ": " + rootMessage;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return cursor;
    }

    private String stackTrace(Exception exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private String truncate(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit - 1) + "…";
    }

    public record IncidentDashboardSummary(long openCount, long activeCount) {
    }

    public record IncidentRow(
        Long incidentId,
        String code,
        AppIncidentArea area,
        AppIncidentSeverity severity,
        AppIncidentStatus status,
        String message,
        String technicalDetail,
        String stackTrace,
        String method,
        String path,
        String username,
        String userAgent,
        Long bookingId,
        Long guestId,
        Long communicationId,
        OffsetDateTime createdAt,
        OffsetDateTime reviewedAt,
        OffsetDateTime resolvedAt
    ) {
        public String toneClass() {
            return switch (status) {
                case OPEN -> severity == AppIncidentSeverity.ERROR ? "mono-pill-danger" : "mono-pill-warning";
                case REVIEWED -> "mono-pill-warning";
                case RESOLVED -> "mono-pill-success";
            };
        }

        public boolean canMarkReviewed() {
            return status == AppIncidentStatus.OPEN;
        }

        public boolean canMarkResolved() {
            return status != AppIncidentStatus.RESOLVED;
        }
    }
}
