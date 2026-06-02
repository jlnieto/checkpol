package es.checkpol.domain.billing;

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
@Table(name = "stripe_event_logs")
public class StripeEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stripe_event_id", nullable = false, unique = true, length = 120)
    private String stripeEventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private StripeEventProcessingStatus processingStatus;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected StripeEventLog() {
    }

    public StripeEventLog(String stripeEventId, String eventType, String payload, OffsetDateTime createdAt) {
        this.stripeEventId = stripeEventId;
        this.eventType = eventType;
        this.payload = payload;
        this.processingStatus = StripeEventProcessingStatus.RECEIVED;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getStripeEventId() {
        return stripeEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public StripeEventProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getPayload() {
        return payload;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void markProcessed(OffsetDateTime processedAt) {
        this.processingStatus = StripeEventProcessingStatus.PROCESSED;
        this.processedAt = processedAt;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage, OffsetDateTime processedAt) {
        this.processingStatus = StripeEventProcessingStatus.FAILED;
        this.processedAt = processedAt;
        this.errorMessage = errorMessage;
    }
}
