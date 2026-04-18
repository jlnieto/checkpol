package es.checkpol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "app_incidents")
public class AppIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppIncidentArea area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppIncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppIncidentStatus status;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(name = "technical_detail", length = 2000)
    private String technicalDetail;

    @Column(name = "stack_trace", columnDefinition = "text")
    private String stackTrace;

    @Column(nullable = false, length = 12)
    private String method;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(length = 120)
    private String username;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "guest_id")
    private Long guestId;

    @Column(name = "communication_id")
    private Long communicationId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    protected AppIncident() {
    }

    public AppIncident(
        AppIncidentArea area,
        AppIncidentSeverity severity,
        String message,
        String technicalDetail,
        String stackTrace,
        String method,
        String path,
        String username,
        String userAgent,
        Long ownerId,
        Long bookingId,
        Long guestId,
        Long communicationId,
        OffsetDateTime createdAt
    ) {
        this.area = area;
        this.severity = severity;
        this.status = AppIncidentStatus.OPEN;
        this.message = message;
        this.technicalDetail = technicalDetail;
        this.stackTrace = stackTrace;
        this.method = method;
        this.path = path;
        this.username = username;
        this.userAgent = userAgent;
        this.ownerId = ownerId;
        this.bookingId = bookingId;
        this.guestId = guestId;
        this.communicationId = communicationId;
        this.createdAt = createdAt;
    }

    public void markReviewed(OffsetDateTime reviewedAt) {
        if (status == AppIncidentStatus.OPEN) {
            status = AppIncidentStatus.REVIEWED;
        }
        this.reviewedAt = reviewedAt;
    }

    public void markResolved(OffsetDateTime resolvedAt) {
        status = AppIncidentStatus.RESOLVED;
        this.resolvedAt = resolvedAt;
        if (reviewedAt == null) {
            reviewedAt = resolvedAt;
        }
    }

    public Long getId() {
        return id;
    }

    public AppIncidentArea getArea() {
        return area;
    }

    public AppIncidentSeverity getSeverity() {
        return severity;
    }

    public AppIncidentStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getTechnicalDetail() {
        return technicalDetail;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getUsername() {
        return username;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getGuestId() {
        return guestId;
    }

    public Long getCommunicationId() {
        return communicationId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getCode() {
        return id == null ? "INC-PEND" : "INC-" + id;
    }
}
