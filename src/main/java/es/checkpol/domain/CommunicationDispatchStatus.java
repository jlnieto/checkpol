package es.checkpol.domain;

public enum CommunicationDispatchStatus {
    XML_READY,
    SUBMITTED_TO_SES,
    SES_RESPONSE_NEEDS_REVIEW,
    SUBMISSION_FAILED,
    SES_PROCESSED,
    SES_PROCESSING_ERROR,
    SES_CANCELLED
}
