package es.checkpol.service;

import es.checkpol.domain.SesConnectionTestStatus;

import java.time.OffsetDateTime;

public record SesConnectionTestResult(
    SesConnectionTestStatus status,
    String ownerMessage,
    String adminMessage,
    OffsetDateTime testedAt,
    String endpointUrl,
    Integer httpStatus,
    String errorType,
    String rawDetail
) {

    public boolean success() {
        return status == SesConnectionTestStatus.OK;
    }

    public boolean technicalIssue() {
        return status == SesConnectionTestStatus.TECHNICAL_ERROR;
    }

    public boolean accessIssue() {
        return status == SesConnectionTestStatus.ACCESS_ERROR;
    }

    public boolean configurationIncomplete() {
        return status == SesConnectionTestStatus.CONFIGURATION_INCOMPLETE;
    }
}
