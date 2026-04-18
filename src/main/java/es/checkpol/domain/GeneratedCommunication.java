package es.checkpol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "generated_communications")
public class GeneratedCommunication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "last_downloaded_at")
    private OffsetDateTime lastDownloadedAt;

    @Column(name = "download_count", nullable = false)
    private Integer downloadCount;

    @Column(name = "xml_content", nullable = false)
    private String xmlContent;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "dispatch_mode", nullable = false, length = 30)
    private CommunicationDispatchMode dispatchMode;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "dispatch_status", nullable = false, length = 30)
    private CommunicationDispatchStatus dispatchStatus;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "ses_lote_code", length = 36)
    private String sesLoteCode;

    @Column(name = "ses_response_code")
    private Integer sesResponseCode;

    @Column(name = "ses_response_description", length = 200)
    private String sesResponseDescription;

    @Column(name = "ses_submission_raw_response")
    private String sesSubmissionRawResponse;

    @Column(name = "ses_communication_code", length = 36)
    private String sesCommunicationCode;

    @Column(name = "ses_last_status_checked_at")
    private OffsetDateTime sesLastStatusCheckedAt;

    @Column(name = "ses_processing_state_code")
    private Integer sesProcessingStateCode;

    @Column(name = "ses_processing_state_description", length = 200)
    private String sesProcessingStateDescription;

    @Column(name = "ses_processing_error_type", length = 5)
    private String sesProcessingErrorType;

    @Column(name = "ses_processing_error_description", length = 200)
    private String sesProcessingErrorDescription;

    @Column(name = "ses_status_raw_response")
    private String sesStatusRawResponse;

    @Column(name = "ses_cancelled_at")
    private OffsetDateTime sesCancelledAt;

    @Column(name = "ses_cancellation_raw_response")
    private String sesCancellationRawResponse;

    @Column(name = "ses_problem_reviewed_at")
    private OffsetDateTime sesProblemReviewedAt;

    @Column(name = "ses_problem_reviewed_by", length = 120)
    private String sesProblemReviewedBy;

    protected GeneratedCommunication() {
    }

    public GeneratedCommunication(Booking booking, Integer version, OffsetDateTime generatedAt, String xmlContent) {
        this(booking, version, generatedAt, xmlContent, CommunicationDispatchMode.MANUAL_DOWNLOAD, CommunicationDispatchStatus.XML_READY);
    }

    public GeneratedCommunication(
        Booking booking,
        Integer version,
        OffsetDateTime generatedAt,
        String xmlContent,
        CommunicationDispatchMode dispatchMode,
        CommunicationDispatchStatus dispatchStatus
    ) {
        this.booking = booking;
        this.version = version;
        this.generatedAt = generatedAt;
        this.downloadCount = 0;
        this.xmlContent = xmlContent;
        this.dispatchMode = dispatchMode;
        this.dispatchStatus = dispatchStatus;
    }

    public Long getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public Integer getVersion() {
        return version;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public OffsetDateTime getLastDownloadedAt() {
        return lastDownloadedAt;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public String getXmlContent() {
        return xmlContent;
    }

    public CommunicationDispatchMode getDispatchMode() {
        return dispatchMode;
    }

    public CommunicationDispatchStatus getDispatchStatus() {
        return dispatchStatus;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public String getSesLoteCode() {
        return sesLoteCode;
    }

    public Integer getSesResponseCode() {
        return sesResponseCode;
    }

    public String getSesResponseDescription() {
        return sesResponseDescription;
    }

    public String getSesSubmissionRawResponse() {
        return sesSubmissionRawResponse;
    }

    public String getSesCommunicationCode() {
        return sesCommunicationCode;
    }

    public OffsetDateTime getSesLastStatusCheckedAt() {
        return sesLastStatusCheckedAt;
    }

    public Integer getSesProcessingStateCode() {
        return sesProcessingStateCode;
    }

    public String getSesProcessingStateDescription() {
        return sesProcessingStateDescription;
    }

    public String getSesProcessingErrorType() {
        return sesProcessingErrorType;
    }

    public String getSesProcessingErrorDescription() {
        return sesProcessingErrorDescription;
    }

    public String getSesStatusRawResponse() {
        return sesStatusRawResponse;
    }

    public OffsetDateTime getSesCancelledAt() {
        return sesCancelledAt;
    }

    public String getSesCancellationRawResponse() {
        return sesCancellationRawResponse;
    }

    public OffsetDateTime getSesProblemReviewedAt() {
        return sesProblemReviewedAt;
    }

    public String getSesProblemReviewedBy() {
        return sesProblemReviewedBy;
    }

    public void registerDownload(OffsetDateTime downloadedAt) {
        this.lastDownloadedAt = downloadedAt;
        this.downloadCount = this.downloadCount + 1;
    }

    public void registerSesSubmission(OffsetDateTime submittedAt, String sesLoteCode, Integer sesResponseCode, String sesResponseDescription) {
        registerSesSubmission(submittedAt, sesLoteCode, sesResponseCode, sesResponseDescription, null);
    }

    public void registerSesSubmission(OffsetDateTime submittedAt, String sesLoteCode, Integer sesResponseCode, String sesResponseDescription, String rawResponse) {
        clearSesProblemReview();
        this.dispatchMode = CommunicationDispatchMode.SES_WEB_SERVICE;
        this.submittedAt = submittedAt;
        this.sesLoteCode = sesLoteCode;
        this.sesResponseCode = sesResponseCode;
        this.sesResponseDescription = sesResponseDescription;
        this.sesSubmissionRawResponse = rawResponse;
        if (Integer.valueOf(0).equals(sesResponseCode) && sesLoteCode != null && !sesLoteCode.isBlank()) {
            this.dispatchStatus = CommunicationDispatchStatus.SUBMITTED_TO_SES;
        } else if (Integer.valueOf(0).equals(sesResponseCode)) {
            this.dispatchStatus = CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW;
        } else {
            this.dispatchStatus = CommunicationDispatchStatus.SUBMISSION_FAILED;
        }
    }

    public void registerFailedSesSubmission(OffsetDateTime submittedAt, Integer sesResponseCode, String sesResponseDescription) {
        registerFailedSesSubmission(submittedAt, sesResponseCode, sesResponseDescription, null);
    }

    public void registerFailedSesSubmission(OffsetDateTime submittedAt, Integer sesResponseCode, String sesResponseDescription, String rawResponse) {
        clearSesProblemReview();
        this.dispatchMode = CommunicationDispatchMode.SES_WEB_SERVICE;
        this.dispatchStatus = CommunicationDispatchStatus.SUBMISSION_FAILED;
        this.submittedAt = submittedAt;
        this.sesResponseCode = sesResponseCode;
        this.sesResponseDescription = sesResponseDescription;
        this.sesSubmissionRawResponse = rawResponse;
        this.sesLoteCode = null;
    }

    public void registerSesProcessingResult(
        OffsetDateTime checkedAt,
        Integer processingStateCode,
        String processingStateDescription,
        String communicationCode,
        String processingErrorType,
        String processingErrorDescription
    ) {
        registerSesProcessingResult(
            checkedAt,
            processingStateCode,
            processingStateDescription,
            communicationCode,
            processingErrorType,
            processingErrorDescription,
            this.sesResponseCode,
            this.sesResponseDescription,
            this.sesStatusRawResponse
        );
    }

    public void registerSesProcessingResult(
        OffsetDateTime checkedAt,
        Integer processingStateCode,
        String processingStateDescription,
        String communicationCode,
        String processingErrorType,
        String processingErrorDescription,
        Integer responseCode,
        String responseDescription,
        String rawResponse
    ) {
        clearSesProblemReview();
        this.sesLastStatusCheckedAt = checkedAt;
        this.sesResponseCode = responseCode;
        this.sesResponseDescription = responseDescription;
        this.sesProcessingStateCode = processingStateCode;
        this.sesProcessingStateDescription = processingStateDescription;
        this.sesCommunicationCode = communicationCode;
        this.sesProcessingErrorType = processingErrorType;
        this.sesProcessingErrorDescription = processingErrorDescription;
        this.sesStatusRawResponse = rawResponse;
        this.dispatchMode = CommunicationDispatchMode.SES_WEB_SERVICE;
        if (responseCode != null && responseCode != 0) {
            this.dispatchStatus = CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW;
        } else if (communicationCode != null && !communicationCode.isBlank()) {
            this.dispatchStatus = CommunicationDispatchStatus.SES_PROCESSED;
        } else if (processingErrorType != null && !processingErrorType.isBlank()) {
            this.dispatchStatus = CommunicationDispatchStatus.SES_PROCESSING_ERROR;
        } else {
            this.dispatchStatus = CommunicationDispatchStatus.SUBMITTED_TO_SES;
        }
    }

    public void registerSesResponseNeedsReview(OffsetDateTime checkedAt, Integer responseCode, String responseDescription, String rawResponse) {
        clearSesProblemReview();
        this.dispatchMode = CommunicationDispatchMode.SES_WEB_SERVICE;
        this.dispatchStatus = CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW;
        this.sesLastStatusCheckedAt = checkedAt;
        this.sesResponseCode = responseCode;
        this.sesResponseDescription = responseDescription;
        this.sesStatusRawResponse = rawResponse;
    }

    public void registerSesCancellation(OffsetDateTime cancelledAt, Integer responseCode, String responseDescription) {
        registerSesCancellation(cancelledAt, responseCode, responseDescription, null);
    }

    public void registerSesCancellation(OffsetDateTime cancelledAt, Integer responseCode, String responseDescription, String rawResponse) {
        clearSesProblemReview();
        this.dispatchMode = CommunicationDispatchMode.SES_WEB_SERVICE;
        this.sesResponseCode = responseCode;
        this.sesResponseDescription = responseDescription;
        this.sesCancellationRawResponse = rawResponse;
        if (Integer.valueOf(0).equals(responseCode)) {
            this.dispatchStatus = CommunicationDispatchStatus.SES_CANCELLED;
            this.sesCancelledAt = cancelledAt;
        }
    }

    public boolean isSesProblemStatus() {
        return dispatchStatus == CommunicationDispatchStatus.SUBMISSION_FAILED
            || dispatchStatus == CommunicationDispatchStatus.SES_PROCESSING_ERROR
            || dispatchStatus == CommunicationDispatchStatus.SES_RESPONSE_NEEDS_REVIEW;
    }

    public boolean canMarkSesProblemReviewed() {
        return isSesProblemStatus() && sesProblemReviewedAt == null;
    }

    public void markSesProblemReviewed(OffsetDateTime reviewedAt, String reviewedBy) {
        if (!isSesProblemStatus()) {
            throw new IllegalStateException("Esta comunicación no tiene una incidencia SES activa.");
        }
        this.sesProblemReviewedAt = reviewedAt;
        this.sesProblemReviewedBy = reviewedBy;
    }

    private void clearSesProblemReview() {
        this.sesProblemReviewedAt = null;
        this.sesProblemReviewedBy = null;
    }
}
