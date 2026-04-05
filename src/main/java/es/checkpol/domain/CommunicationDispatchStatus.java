package es.checkpol.domain;

public enum CommunicationDispatchStatus {
    XML_READY,
    SUBMITTED_TO_SES,
    SUBMISSION_FAILED,
    SES_PROCESSED,
    SES_PROCESSING_ERROR,
    SES_CANCELLED
}
