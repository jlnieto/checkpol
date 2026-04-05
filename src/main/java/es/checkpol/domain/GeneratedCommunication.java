package es.checkpol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Lob;
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

    @Lob
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

    @Column(name = "ses_cancelled_at")
    private OffsetDateTime sesCancelledAt;

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

    public OffsetDateTime getSesCancelledAt() {
        return sesCancelledAt;
    }

    public void registerDownload(OffsetDateTime downloadedAt) {
        this.lastDownloadedAt = downloadedAt;
        this.downloadCount = this.downloadCount + 1;
    }

    public void registerSesSubmission(OffsetDateTime submittedAt, String sesLoteCode, Integer sesResponseCode, String sesResponseDescription) {
        this.dispatchMode = CommunicationDispatchMode.SES_WEB_SERVICE;
        this.dispatchStatus = CommunicationDispatchStatus.SUBMITTED_TO_SES;
        this.submittedAt = submittedAt;
        this.sesLoteCode = sesLoteCode;
        this.sesResponseCode = sesResponseCode;
        this.sesResponseDescription = sesResponseDescription;
    }

    public void registerFailedSesSubmission(OffsetDateTime submittedAt, Integer sesResponseCode, String sesResponseDescription) {
        this.dispatchMode = CommunicationDispatchMode.SES_WEB_SERVICE;
        this.dispatchStatus = CommunicationDispatchStatus.SUBMISSION_FAILED;
        this.submittedAt = submittedAt;
        this.sesResponseCode = sesResponseCode;
        this.sesResponseDescription = sesResponseDescription;
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
        this.sesLastStatusCheckedAt = checkedAt;
        this.sesProcessingStateCode = processingStateCode;
        this.sesProcessingStateDescription = processingStateDescription;
        this.sesCommunicationCode = communicationCode;
        this.sesProcessingErrorType = processingErrorType;
        this.sesProcessingErrorDescription = processingErrorDescription;
        this.dispatchMode = CommunicationDispatchMode.SES_WEB_SERVICE;
        if (communicationCode != null && !communicationCode.isBlank()) {
            this.dispatchStatus = CommunicationDispatchStatus.SES_PROCESSED;
        } else if (processingErrorType != null && !processingErrorType.isBlank()) {
            this.dispatchStatus = CommunicationDispatchStatus.SES_PROCESSING_ERROR;
        } else {
            this.dispatchStatus = CommunicationDispatchStatus.SUBMITTED_TO_SES;
        }
    }

    public void registerSesCancellation(OffsetDateTime cancelledAt, Integer responseCode, String responseDescription) {
        this.dispatchMode = CommunicationDispatchMode.SES_WEB_SERVICE;
        this.dispatchStatus = CommunicationDispatchStatus.SES_CANCELLED;
        this.sesCancelledAt = cancelledAt;
        this.sesResponseCode = responseCode;
        this.sesResponseDescription = responseDescription;
    }
}
