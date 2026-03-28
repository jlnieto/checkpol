package es.checkpol.service;

import es.checkpol.domain.Guest;
import es.checkpol.domain.MunicipalityIssueStatus;
import es.checkpol.domain.MunicipalityResolutionIssue;
import es.checkpol.domain.MunicipalityResolutionRule;
import es.checkpol.domain.MunicipalityResolutionStatus;
import es.checkpol.repository.GuestRepository;
import es.checkpol.repository.MunicipalityResolutionIssueRepository;
import es.checkpol.repository.MunicipalityResolutionRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class MunicipalityReviewService {

    private static final Pattern MUNICIPALITY_CODE_PATTERN = Pattern.compile("^\\d{5}$");

    private final MunicipalityResolutionIssueRepository issueRepository;
    private final MunicipalityResolutionRuleRepository ruleRepository;
    private final GuestRepository guestRepository;

    public MunicipalityReviewService(
        MunicipalityResolutionIssueRepository issueRepository,
        MunicipalityResolutionRuleRepository ruleRepository,
        GuestRepository guestRepository
    ) {
        this.issueRepository = issueRepository;
        this.ruleRepository = ruleRepository;
        this.guestRepository = guestRepository;
    }

    @Transactional
    public void registerAutomaticResolution(Guest guest, MunicipalityResolution resolution) {
        if (!"ESP".equalsIgnoreCase(guest.getCountry())) {
            closeOpenIssue(guest);
            return;
        }

        if (!resolution.status().requiresReview()) {
            closeOpenIssue(guest);
            return;
        }

        String municipalityQueryLabel = normalizeResolutionLabel(
            resolution.municipalityQueryLabel(),
            resolution.municipalityResolvedName(),
            resolution.municipalityCode()
        );
        String municipalityQueryNormalized = normalizeResolutionQuery(
            resolution.municipalityQueryNormalized(),
            municipalityQueryLabel
        );

        MunicipalityResolutionIssue issue = issueRepository.findFirstByGuestIdAndIssueStatus(guest.getId(), MunicipalityIssueStatus.OPEN)
            .orElseGet(() -> new MunicipalityResolutionIssue(
                guest,
                guest.getCountry(),
                guest.getPostalCode(),
                resolution.postalCodePrefix(),
                municipalityQueryNormalized,
                municipalityQueryLabel,
                resolution.municipalityCode(),
                resolution.municipalityResolvedName(),
                resolution.status(),
                resolution.note(),
                MunicipalityIssueStatus.OPEN,
                OffsetDateTime.now(),
                null
            ));

        issue.refreshAutomaticAssignment(
            guest.getPostalCode(),
            resolution.postalCodePrefix(),
            municipalityQueryNormalized,
            municipalityQueryLabel,
            resolution.municipalityCode(),
            resolution.municipalityResolvedName(),
            resolution.status(),
            resolution.note()
        );
        issueRepository.save(issue);
    }

    @Transactional(readOnly = true)
    public MunicipalityAdminDashboard getDashboard() {
        List<MunicipalityIssueSummary> openIssues = issueRepository.findAllByIssueStatusOrderByCreatedAtDesc(MunicipalityIssueStatus.OPEN).stream()
            .map(issue -> new MunicipalityIssueSummary(
                issue.getId(),
                issue.getGuest().getBooking().getId(),
                issue.getGuest().getBooking().getReferenceCode(),
                issue.getGuest().getBooking().getAccommodation().getName(),
                issue.getGuest().getDisplayName(),
                issue.getPostalCode(),
                issue.getMunicipalityQueryLabel(),
                issue.getAssignedMunicipalityCode(),
                issue.getAssignedMunicipalityName(),
                issue.getResolutionStatus(),
                issue.getResolutionNote(),
                issue.getCreatedAt()
            ))
            .toList();

        List<MunicipalityRuleSummary> learnedRules = ruleRepository.findAllByOrderByUpdatedAtDesc().stream()
            .map(rule -> new MunicipalityRuleSummary(
                rule.getCountryCode(),
                rule.getPostalCodePrefix(),
                rule.getMunicipalityQueryLabel(),
                rule.getMunicipalityCode(),
                rule.getMunicipalityName(),
                rule.getUpdatedAt()
            ))
            .toList();

        return new MunicipalityAdminDashboard(openIssues, learnedRules);
    }

    @Transactional
    public void correctIssue(Long issueId, String municipalityCode, String municipalityName, String resolutionNote) {
        if (municipalityCode == null || !MUNICIPALITY_CODE_PATTERN.matcher(municipalityCode.trim()).matches()) {
            throw new IllegalArgumentException("El codigo de municipio debe tener 5 numeros.");
        }
        if (municipalityName == null || municipalityName.isBlank()) {
            throw new IllegalArgumentException("Indica el nombre oficial del municipio.");
        }

        MunicipalityResolutionIssue issue = issueRepository.findById(issueId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado esa incidencia."));

        String normalizedCode = municipalityCode.trim();
        String normalizedName = municipalityName.trim();
        String normalizedNote = resolutionNote == null || resolutionNote.isBlank()
            ? "Correccion manual desde administracion."
            : resolutionNote.trim();

        Guest guest = guestRepository.findById(issue.getGuest().getId())
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado la persona asociada a esta incidencia."));

        guest.applyMunicipalityResolution(
            normalizedCode,
            normalizedName,
            MunicipalityResolutionStatus.MANUAL_OVERRIDE,
            normalizedNote
        );

        MunicipalityResolutionRule rule = ruleRepository
            .findFirstByCountryCodeAndPostalCodePrefixAndMunicipalityQueryNormalized(
                issue.getCountryCode(),
                issue.getPostalCodePrefix(),
                issue.getMunicipalityQueryNormalized()
            )
            .orElseGet(() -> new MunicipalityResolutionRule(
                issue.getCountryCode(),
                issue.getPostalCodePrefix(),
                issue.getMunicipalityQueryNormalized(),
                issue.getMunicipalityQueryLabel(),
                normalizedCode,
                normalizedName,
                OffsetDateTime.now(),
                OffsetDateTime.now()
            ));

        rule.updateResolution(normalizedCode, normalizedName, OffsetDateTime.now());
        ruleRepository.save(rule);

        issue.markResolved(normalizedCode, normalizedName, normalizedNote, OffsetDateTime.now());
    }

    private void closeOpenIssue(Guest guest) {
        issueRepository.findFirstByGuestIdAndIssueStatus(guest.getId(), MunicipalityIssueStatus.OPEN)
            .ifPresent(issue -> issue.closeSilently(OffsetDateTime.now()));
    }

    private String normalizeResolutionLabel(String queryLabel, String resolvedName, String municipalityCode) {
        String label = blankToNull(queryLabel);
        if (label != null) {
            return label;
        }
        label = blankToNull(resolvedName);
        if (label != null) {
            return label;
        }
        return municipalityCode == null || municipalityCode.isBlank() ? "Municipio no indicado" : municipalityCode.trim();
    }

    private String normalizeResolutionQuery(String queryNormalized, String fallbackLabel) {
        String normalized = blankToNull(queryNormalized);
        if (normalized != null) {
            return normalized;
        }

        String fallback = normalizeText(fallbackLabel);
        return fallback == null ? "municipio no indicado" : fallback;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replaceAll("[^A-Za-z0-9 ]", " ")
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ")
            .trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
