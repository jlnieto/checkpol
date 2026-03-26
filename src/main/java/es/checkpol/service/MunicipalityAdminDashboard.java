package es.checkpol.service;

import java.util.List;

public record MunicipalityAdminDashboard(
    List<MunicipalityIssueSummary> openIssues,
    List<MunicipalityRuleSummary> learnedRules
) {
}
