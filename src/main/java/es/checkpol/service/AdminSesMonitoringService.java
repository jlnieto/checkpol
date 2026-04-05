package es.checkpol.service;

import es.checkpol.domain.AppUser;
import es.checkpol.domain.AppUserRole;
import es.checkpol.domain.CommunicationDispatchMode;
import es.checkpol.domain.CommunicationDispatchStatus;
import es.checkpol.domain.GeneratedCommunication;
import es.checkpol.domain.SesConnectionTestStatus;
import es.checkpol.infrastructure.ses.SesWsSslContextFactory;
import es.checkpol.repository.AppUserRepository;
import es.checkpol.repository.GeneratedCommunicationRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AdminSesMonitoringService {

    private static final List<CommunicationDispatchStatus> PROBLEM_STATUSES = List.of(
        CommunicationDispatchStatus.SUBMISSION_FAILED,
        CommunicationDispatchStatus.SES_PROCESSING_ERROR
    );

    private final GeneratedCommunicationRepository generatedCommunicationRepository;
    private final AppUserRepository appUserRepository;
    private final SesCommunicationGateway sesCommunicationGateway;
    private final ResourceLoader resourceLoader;
    private final SesWsSslContextFactory sesWsSslContextFactory;
    private final ObjectProvider<SesSubmissionPollingScheduler> pollingSchedulerProvider;
    private final String endpointUrl;
    private final String truststorePath;
    private final String truststorePassword;
    private final String truststoreType;
    private final long pollingDelayMs;

    public AdminSesMonitoringService(
        GeneratedCommunicationRepository generatedCommunicationRepository,
        AppUserRepository appUserRepository,
        SesCommunicationGateway sesCommunicationGateway,
        ResourceLoader resourceLoader,
        SesWsSslContextFactory sesWsSslContextFactory,
        ObjectProvider<SesSubmissionPollingScheduler> pollingSchedulerProvider,
        @Value("${checkpol.ses.ws.url:}") String endpointUrl,
        @Value("${checkpol.ses.ws.truststore-path:}") String truststorePath,
        @Value("${checkpol.ses.ws.truststore-password:}") String truststorePassword,
        @Value("${checkpol.ses.ws.truststore-type:PKCS12}") String truststoreType,
        @Value("${checkpol.ses.status-poll.delay-ms:60000}") long pollingDelayMs
    ) {
        this.generatedCommunicationRepository = generatedCommunicationRepository;
        this.appUserRepository = appUserRepository;
        this.sesCommunicationGateway = sesCommunicationGateway;
        this.resourceLoader = resourceLoader;
        this.sesWsSslContextFactory = sesWsSslContextFactory;
        this.pollingSchedulerProvider = pollingSchedulerProvider;
        this.endpointUrl = endpointUrl;
        this.truststorePath = truststorePath;
        this.truststorePassword = truststorePassword;
        this.truststoreType = truststoreType;
        this.pollingDelayMs = pollingDelayMs;
    }

    @Transactional(readOnly = true)
    public SesDashboardSummary getDashboardSummary() {
        List<AppUser> owners = appUserRepository.findAllByRoleOrderByDisplayNameAscUsernameAsc(AppUserRole.OWNER);
        long ownersWithoutReadyWs = owners.stream()
            .filter(AppUser::isActive)
            .filter(owner -> !owner.hasSesWebServiceConfiguration())
            .count();

        return new SesDashboardSummary(
            generatedCommunicationRepository.countByDispatchStatus(CommunicationDispatchStatus.SUBMITTED_TO_SES),
            generatedCommunicationRepository.countByDispatchStatus(CommunicationDispatchStatus.SUBMISSION_FAILED),
            generatedCommunicationRepository.countByDispatchStatus(CommunicationDispatchStatus.SES_PROCESSING_ERROR),
            ownersWithoutReadyWs,
            buildTechnicalHealth()
        );
    }

    @Transactional(readOnly = true)
    public List<SesCommunicationRow> findRecentCommunications(String statusFilter, boolean problemOnly) {
        List<GeneratedCommunication> communications = loadCommunications(statusFilter, problemOnly);
        return communications.stream()
            .map(this::toCommunicationRow)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SesOwnerRow> findOwnerRows() {
        return appUserRepository.findAllByRoleOrderByDisplayNameAscUsernameAsc(AppUserRole.OWNER).stream()
            .map(this::toOwnerRow)
            .toList();
    }

    @Transactional
    public SesLoteStatusResult refreshCommunication(Long communicationId) {
        GeneratedCommunication communication = generatedCommunicationRepository.findAdminDetailById(communicationId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado la comunicación SES indicada."));

        if (communication.getSesLoteCode() == null || communication.getSesLoteCode().isBlank()) {
            throw new IllegalStateException("Esta comunicación todavía no tiene lote de SES para consultar.");
        }
        if (!communication.getBooking().getOwner().hasSesWebServiceConfiguration()) {
            throw new IllegalStateException("El owner no tiene la configuración WS completa.");
        }

        SesLoteStatusResult result = sesCommunicationGateway.queryLoteStatus(
            communication.getBooking().getOwner(),
            communication.getSesLoteCode()
        );
        communication.registerSesProcessingResult(
            OffsetDateTime.now(),
            result.processingStateCode(),
            result.processingStateDescription(),
            result.communicationCode(),
            result.processingErrorType(),
            result.processingErrorDescription()
        );
        return result;
    }

    @Transactional
    public GeneratedCommunication retryCommunication(Long communicationId) {
        GeneratedCommunication source = generatedCommunicationRepository.findAdminDetailById(communicationId)
            .orElseThrow(() -> new IllegalArgumentException("No he encontrado la comunicación SES indicada."));

        AppUser owner = source.getBooking().getOwner();
        if (!owner.hasSesWebServiceConfiguration()) {
            throw new IllegalStateException("El owner no tiene la configuración WS completa.");
        }

        int nextVersion = generatedCommunicationRepository.findFirstByBookingIdOrderByVersionDesc(source.getBooking().getId())
            .map(communication -> communication.getVersion() + 1)
            .orElse(1);
        GeneratedCommunication retry = new GeneratedCommunication(
            source.getBooking(),
            nextVersion,
            OffsetDateTime.now(),
            source.getXmlContent(),
            CommunicationDispatchMode.SES_WEB_SERVICE,
            CommunicationDispatchStatus.XML_READY
        );
        generatedCommunicationRepository.save(retry);

        try {
            SesSubmissionResult result = sesCommunicationGateway.submitTravelerPart(owner, retry.getXmlContent());
            retry.registerSesSubmission(OffsetDateTime.now(), result.loteCode(), result.responseCode(), result.responseDescription());
            return retry;
        } catch (RuntimeException exception) {
            retry.registerFailedSesSubmission(OffsetDateTime.now(), null, exception.getMessage());
            throw exception;
        }
    }

    private List<GeneratedCommunication> loadCommunications(String statusFilter, boolean problemOnly) {
        CommunicationDispatchStatus requestedStatus = parseStatus(statusFilter);
        if (requestedStatus != null) {
            return generatedCommunicationRepository.findTop100ByDispatchStatusOrderByGeneratedAtDesc(requestedStatus);
        }
        if (problemOnly) {
            return generatedCommunicationRepository.findTop100ByDispatchStatusInOrderByGeneratedAtDesc(PROBLEM_STATUSES);
        }
        return generatedCommunicationRepository.findTop100ByOrderByGeneratedAtDesc();
    }

    private SesCommunicationRow toCommunicationRow(GeneratedCommunication communication) {
        AppUser owner = communication.getBooking().getOwner();
        return new SesCommunicationRow(
            communication.getId(),
            communication.getBooking().getId(),
            owner.getId(),
            owner.getDisplayName(),
            owner.getUsername(),
            communication.getBooking().getAccommodation().getName(),
            communication.getBooking().getReferenceCode(),
            communication.getVersion(),
            communication.getDispatchMode(),
            communication.getDispatchStatus(),
            communicationStatusLabel(communication),
            communicationToneClass(communication),
            communicationDetail(communication),
            communication.getSesLoteCode(),
            communication.getSesCommunicationCode(),
            communication.getGeneratedAt(),
            communication.getSubmittedAt(),
            communication.getSesLastStatusCheckedAt(),
            communication.getSesCancelledAt(),
            communication.getDispatchStatus() == CommunicationDispatchStatus.SUBMITTED_TO_SES
                && communication.getSesLoteCode() != null
                && owner.hasSesWebServiceConfiguration(),
            communication.getDispatchStatus() == CommunicationDispatchStatus.SUBMISSION_FAILED
                || communication.getDispatchStatus() == CommunicationDispatchStatus.SES_PROCESSING_ERROR
                || communication.getDispatchStatus() == CommunicationDispatchStatus.SES_CANCELLED,
            owner.hasSesWebServiceConfiguration()
        );
    }

    private SesOwnerRow toOwnerRow(AppUser owner) {
        boolean hasArrendadorCode = hasText(owner.getSesArrendadorCode());
        boolean hasWsUsername = hasText(owner.getSesWsUsername());
        boolean hasWsPassword = hasText(owner.getSesWsPasswordEncrypted());
        OwnerConfigurationStatus configurationStatus = resolveOwnerConfigurationStatus(hasArrendadorCode, hasWsUsername, hasWsPassword);

        OffsetDateTime lastSuccessfulSubmissionAt = generatedCommunicationRepository
            .findFirstByBookingOwnerIdAndDispatchStatusOrderBySubmittedAtDesc(owner.getId(), CommunicationDispatchStatus.SES_PROCESSED)
            .map(communication -> communication.getSesLastStatusCheckedAt() != null ? communication.getSesLastStatusCheckedAt() : communication.getSubmittedAt())
            .orElse(null);

        long pendingCount = generatedCommunicationRepository.countByBookingOwnerIdAndDispatchStatus(owner.getId(), CommunicationDispatchStatus.SUBMITTED_TO_SES);
        long errorCount = generatedCommunicationRepository.countByBookingOwnerIdAndDispatchStatusIn(owner.getId(), PROBLEM_STATUSES);
        String lastError = generatedCommunicationRepository
            .findFirstByBookingOwnerIdAndDispatchStatusInOrderByGeneratedAtDesc(owner.getId(), PROBLEM_STATUSES)
            .map(this::communicationDetail)
            .orElse(null);

        return new SesOwnerRow(
            owner.getId(),
            owner.getDisplayName(),
            owner.getUsername(),
            owner.isActive(),
            configurationStatus.label(),
            configurationStatus.toneClass(),
            configurationStatus.detail(),
            owner.getSesArrendadorCode(),
            owner.getSesWsUsername(),
            hasWsPassword,
            owner.getSesConnectionTestStatus(),
            owner.getSesConnectionTestedAt(),
            normalizeDetail(owner.getSesConnectionAdminMessage(), "Sin prueba de conexión todavía."),
            owner.getSesConnectionTestHttpStatus(),
            owner.getSesConnectionTestErrorType(),
            lastSuccessfulSubmissionAt,
            lastError,
            pendingCount,
            errorCount
        );
    }

    private SesTechnicalHealth buildTechnicalHealth() {
        SesTechnicalCheck endpointCheck = hasText(endpointUrl)
            ? new SesTechnicalCheck("Endpoint SES", "Configurado", normalizeDetail(endpointUrl), "mono-pill-success")
            : new SesTechnicalCheck("Endpoint SES", "Falta", "No hay endpoint WS configurado.", "mono-pill-warning");

        SesTechnicalCheck truststoreCheck;
        if (!hasText(truststorePath)) {
            truststoreCheck = new SesTechnicalCheck("Truststore", "Falta", "No hay truststore configurado. Solo el modo manual es seguro ahora mismo.", "mono-pill-warning");
        } else {
            Resource resource = resourceLoader.getResource(truststorePath);
            if (!resource.exists()) {
                truststoreCheck = new SesTechnicalCheck("Truststore", "Error", "La ruta configurada no existe: " + truststorePath, "mono-pill-danger");
            } else {
                try {
                    sesWsSslContextFactory.build(resource, truststorePassword, truststoreType);
                    truststoreCheck = new SesTechnicalCheck("Truststore", "OK", "Cargado correctamente desde " + truststorePath, "mono-pill-success");
                } catch (RuntimeException exception) {
                    truststoreCheck = new SesTechnicalCheck("Truststore", "Error", exception.getMessage(), "mono-pill-danger");
                }
            }
        }

        SesTechnicalCheck schedulerCheck;
        if (pollingSchedulerProvider.getIfAvailable() == null) {
            schedulerCheck = new SesTechnicalCheck("Scheduler SES", "Error", "No encuentro el programador de reconsulta de lotes.", "mono-pill-danger");
        } else if (pollingDelayMs <= 0) {
            schedulerCheck = new SesTechnicalCheck("Scheduler SES", "Error", "La reconsulta automática está desactivada.", "mono-pill-danger");
        } else {
            schedulerCheck = new SesTechnicalCheck("Scheduler SES", "Activo", "Reconsulta automática cada " + pollingDelayMs / 1000 + " s.", "mono-pill-success");
        }

        String overallLabel = "Controlado";
        String overallToneClass = "mono-pill-success";
        if ("mono-pill-danger".equals(endpointCheck.toneClass()) || "mono-pill-danger".equals(truststoreCheck.toneClass()) || "mono-pill-danger".equals(schedulerCheck.toneClass())) {
            overallLabel = "Con incidencias";
            overallToneClass = "mono-pill-danger";
        } else if ("mono-pill-warning".equals(endpointCheck.toneClass()) || "mono-pill-warning".equals(truststoreCheck.toneClass()) || "mono-pill-warning".equals(schedulerCheck.toneClass())) {
            overallLabel = "Parcial";
            overallToneClass = "mono-pill-warning";
        }

        return new SesTechnicalHealth(overallLabel, overallToneClass, endpointCheck, truststoreCheck, schedulerCheck);
    }

    private CommunicationDispatchStatus parseStatus(String statusFilter) {
        if (!hasText(statusFilter) || "ALL".equalsIgnoreCase(statusFilter)) {
            return null;
        }
        try {
            return CommunicationDispatchStatus.valueOf(statusFilter.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private OwnerConfigurationStatus resolveOwnerConfigurationStatus(boolean hasArrendadorCode, boolean hasWsUsername, boolean hasWsPassword) {
        if (hasArrendadorCode && hasWsUsername && hasWsPassword) {
            return new OwnerConfigurationStatus("Listo para WS", "mono-pill-success", "Puede presentar automáticamente en SES.");
        }
        if (!hasArrendadorCode && !hasWsUsername && !hasWsPassword) {
            return new OwnerConfigurationStatus("Solo manual", "mono-pill-neutral", "Aún no tiene datos WS. Solo podrá descargar XML.");
        }
        return new OwnerConfigurationStatus("Incompleto", "mono-pill-warning", "Falta algún dato WS y conviene corregirlo cuanto antes.");
    }

    private String communicationStatusLabel(GeneratedCommunication communication) {
        return switch (communication.getDispatchStatus()) {
            case XML_READY -> communication.getDispatchMode() == CommunicationDispatchMode.MANUAL_DOWNLOAD ? "Solo XML" : "Lista para reintento";
            case SUBMITTED_TO_SES -> "Pendiente SES";
            case SUBMISSION_FAILED -> "Error envío";
            case SES_PROCESSED -> "SES ok";
            case SES_PROCESSING_ERROR -> "Error SES";
            case SES_CANCELLED -> "Anulada";
        };
    }

    private String communicationToneClass(GeneratedCommunication communication) {
        return switch (communication.getDispatchStatus()) {
            case SES_PROCESSED -> "mono-pill-success";
            case SUBMITTED_TO_SES -> "mono-pill-warning";
            case SUBMISSION_FAILED, SES_PROCESSING_ERROR -> "mono-pill-danger";
            case XML_READY, SES_CANCELLED -> "mono-pill-neutral";
        };
    }

    private String communicationDetail(GeneratedCommunication communication) {
        return switch (communication.getDispatchStatus()) {
            case SUBMISSION_FAILED -> normalizeDetail(communication.getSesResponseDescription(), "El envío falló antes de llegar a SES.");
            case SES_PROCESSING_ERROR -> normalizeDetail(communication.getSesProcessingErrorDescription(), "SES ha rechazado la comunicación.");
            case SES_PROCESSED -> normalizeDetail(
                communication.getSesCommunicationCode() != null ? "Código SES " + communication.getSesCommunicationCode() : communication.getSesProcessingStateDescription(),
                "SES ya ha confirmado la comunicación."
            );
            case SUBMITTED_TO_SES -> normalizeDetail(
                communication.getSesLoteCode() != null ? "Lote " + communication.getSesLoteCode() + " pendiente de procesar." : null,
                "Pendiente de lote."
            );
            case SES_CANCELLED -> normalizeDetail(communication.getSesResponseDescription(), "Lote anulado en SES.");
            case XML_READY -> communication.getDispatchMode() == CommunicationDispatchMode.MANUAL_DOWNLOAD
                ? "Se generó para descarga manual."
                : "Lista para presentar o reintentar.";
        };
    }

    private String normalizeDetail(String detail) {
        return normalizeDetail(detail, "Sin detalle");
    }

    private String normalizeDetail(String detail, String fallback) {
        return hasText(detail) ? detail.trim() : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record SesDashboardSummary(
        long pendingCount,
        long submissionErrorCount,
        long processingErrorCount,
        long ownersWithoutReadyWsCount,
        SesTechnicalHealth technicalHealth
    ) {
        public long alertCount() {
            return submissionErrorCount + processingErrorCount + ownersWithoutReadyWsCount;
        }
    }

    public record SesCommunicationRow(
        Long communicationId,
        Long bookingId,
        Long ownerId,
        String ownerDisplayName,
        String ownerUsername,
        String accommodationName,
        String bookingReference,
        Integer version,
        CommunicationDispatchMode dispatchMode,
        CommunicationDispatchStatus dispatchStatus,
        String statusLabel,
        String toneClass,
        String detail,
        String loteCode,
        String sesCommunicationCode,
        OffsetDateTime generatedAt,
        OffsetDateTime submittedAt,
        OffsetDateTime lastCheckedAt,
        OffsetDateTime cancelledAt,
        boolean canRefresh,
        boolean canRetry,
        boolean ownerReadyForWs
    ) {
    }

    public record SesOwnerRow(
        Long ownerId,
        String displayName,
        String username,
        boolean active,
        String configurationLabel,
        String configurationToneClass,
        String configurationDetail,
        String arrendadorCode,
        String wsUsername,
        boolean hasStoredPassword,
        SesConnectionTestStatus connectionTestStatus,
        OffsetDateTime connectionTestedAt,
        String connectionAdminMessage,
        Integer connectionHttpStatus,
        String connectionErrorType,
        OffsetDateTime lastSuccessfulSubmissionAt,
        String lastError,
        long pendingCount,
        long errorCount
    ) {
        public String connectionLabel() {
            if (connectionTestStatus == null || connectionTestStatus == SesConnectionTestStatus.NOT_TESTED) {
                return "Sin probar";
            }
            return switch (connectionTestStatus) {
                case OK -> "Conexión OK";
                case CONFIGURATION_INCOMPLETE -> "Faltan datos";
                case ACCESS_ERROR -> "Revisar acceso";
                case TECHNICAL_ERROR -> "Error técnico";
                case NOT_TESTED -> "Sin probar";
            };
        }

        public String connectionToneClass() {
            if (connectionTestStatus == null || connectionTestStatus == SesConnectionTestStatus.NOT_TESTED) {
                return "mono-pill-neutral";
            }
            return switch (connectionTestStatus) {
                case OK -> "mono-pill-success";
                case CONFIGURATION_INCOMPLETE, ACCESS_ERROR -> "mono-pill-warning";
                case TECHNICAL_ERROR -> "mono-pill-danger";
                case NOT_TESTED -> "mono-pill-neutral";
            };
        }

        public String operationalLabel() {
            if (!active) {
                return "Inactivo";
            }
            if ("Incompleto".equals(configurationLabel)) {
                return "Configurar";
            }
            if (errorCount > 0) {
                return "Con errores";
            }
            if (pendingCount > 0) {
                return "Pendiente";
            }
            if ("Listo para WS".equals(configurationLabel)) {
                return "Listo";
            }
            return "Manual";
        }

        public String operationalToneClass() {
            if (!active) {
                return "mono-pill-neutral";
            }
            if ("Incompleto".equals(configurationLabel)) {
                return "mono-pill-warning";
            }
            if (errorCount > 0) {
                return "mono-pill-danger";
            }
            if (pendingCount > 0) {
                return "mono-pill-warning";
            }
            if ("Listo para WS".equals(configurationLabel)) {
                return "mono-pill-success";
            }
            return "mono-pill-neutral";
        }
    }

    public record SesTechnicalHealth(
        String overallLabel,
        String overallToneClass,
        SesTechnicalCheck endpoint,
        SesTechnicalCheck truststore,
        SesTechnicalCheck scheduler
    ) {
    }

    public record SesTechnicalCheck(
        String label,
        String statusLabel,
        String detail,
        String toneClass
    ) {
    }

    private record OwnerConfigurationStatus(String label, String toneClass, String detail) {
    }
}
