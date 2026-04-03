package es.checkpol.service;

import es.checkpol.domain.MunicipalityCatalogEntry;
import es.checkpol.domain.MunicipalityImportOperation;
import es.checkpol.domain.MunicipalityImportRecord;
import es.checkpol.domain.MunicipalityImportStatus;
import es.checkpol.repository.MunicipalityCatalogEntryRepository;
import es.checkpol.repository.MunicipalityImportRecordRepository;
import es.checkpol.repository.PostalCodeMunicipalityMappingRepository;
import es.checkpol.web.AdminMunicipalityImportForm;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class MunicipalityAdminService {

    private static final String SPAIN = "ESP";
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(3);
    private static final Duration VERIFICATION_STALE_AFTER = Duration.ofDays(14);
    private static final Duration PREVIEW_STALE_AFTER = Duration.ofHours(12);
    private static final int MIN_REASONABLE_MUNICIPALITY_ROWS = 7000;
    private static final int MIN_REASONABLE_POSTAL_MAPPING_ROWS = 10000;
    private static final DateTimeFormatter VERIFICATION_VERSION_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MunicipalityCatalogService municipalityCatalogService;
    private final MunicipalityCatalogImportService municipalityCatalogImportService;
    private final MunicipalityCatalogEntryRepository municipalityCatalogEntryRepository;
    private final PostalCodeMunicipalityMappingRepository postalCodeMunicipalityMappingRepository;
    private final MunicipalityImportRecordRepository municipalityImportRecordRepository;
    private final IneMunicipalityWorkbookParser ineMunicipalityWorkbookParser;
    private final InePostalMappingsZipParser inePostalMappingsZipParser;
    private final AdminSettingsService adminSettingsService;
    private final HttpClient httpClient;

    public MunicipalityAdminService(
        MunicipalityCatalogService municipalityCatalogService,
        MunicipalityCatalogImportService municipalityCatalogImportService,
        MunicipalityCatalogEntryRepository municipalityCatalogEntryRepository,
        PostalCodeMunicipalityMappingRepository postalCodeMunicipalityMappingRepository,
        MunicipalityImportRecordRepository municipalityImportRecordRepository,
        IneMunicipalityWorkbookParser ineMunicipalityWorkbookParser,
        InePostalMappingsZipParser inePostalMappingsZipParser,
        AdminSettingsService adminSettingsService
    ) {
        this.municipalityCatalogService = municipalityCatalogService;
        this.municipalityCatalogImportService = municipalityCatalogImportService;
        this.municipalityCatalogEntryRepository = municipalityCatalogEntryRepository;
        this.postalCodeMunicipalityMappingRepository = postalCodeMunicipalityMappingRepository;
        this.municipalityImportRecordRepository = municipalityImportRecordRepository;
        this.ineMunicipalityWorkbookParser = ineMunicipalityWorkbookParser;
        this.inePostalMappingsZipParser = inePostalMappingsZipParser;
        this.adminSettingsService = adminSettingsService;
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(DOWNLOAD_TIMEOUT)
            .build();
    }

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary() {
        Optional<MunicipalityCatalogEntry> currentDataset = municipalityCatalogEntryRepository.findTopByCountryCodeAndActiveTrueOrderByUpdatedAtDesc(SPAIN);
        Optional<MunicipalityImportRecord> lastImport = municipalityImportRecordRepository.findTopByOperationTypeOrderByCreatedAtDesc(MunicipalityImportOperation.IMPORT);
        Optional<MunicipalityImportRecord> lastVerification = municipalityImportRecordRepository.findTopByOperationTypeOrderByCreatedAtDesc(MunicipalityImportOperation.VERIFY);
        return new DashboardSummary(
            municipalityCatalogService.hasSpanishCatalogData(),
            municipalityCatalogEntryRepository.countByCountryCodeAndActiveTrue(SPAIN),
            postalCodeMunicipalityMappingRepository.countByActiveTrue(),
            currentDataset.map(MunicipalityCatalogEntry::getSource).orElse(null),
            currentDataset.map(MunicipalityCatalogEntry::getSourceVersion).orElse(null),
            currentDataset.map(MunicipalityCatalogEntry::getUpdatedAt).orElse(null),
            buildSourceHealth(lastVerification.orElse(null)),
            lastImport.map(record -> new ImportHistoryItem(
                record.getId(),
                record.getOperationType().name(),
                record.getSource(),
                record.getSourceVersion(),
                record.getStatus().name(),
                record.getImportedMunicipalities(),
                record.getImportedPostalMappings(),
                record.getCreatedAt(),
                record.getTriggeredByUsername(),
                record.getErrorMessage()
            )).orElse(null),
            municipalityImportRecordRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(record -> new ImportHistoryItem(
                    record.getId(),
                    record.getOperationType().name(),
                    record.getSource(),
                    record.getSourceVersion(),
                    record.getStatus().name(),
                    record.getImportedMunicipalities(),
                    record.getImportedPostalMappings(),
                    record.getCreatedAt(),
                    record.getTriggeredByUsername(),
                    record.getErrorMessage()
                ))
                .toList()
        );
    }

    public AdminMunicipalityImportForm defaultForm() {
        AdminSettingsService.MunicipalityAdminDefaults defaults = adminSettingsService.getMunicipalityAdminDefaults();
        return new AdminMunicipalityImportForm(
            defaults.municipalitiesUrl(),
            defaults.postalMappingsUrl(),
            defaults.source(),
            defaultImportVersion()
        );
    }

    @Transactional(readOnly = true)
    public Optional<ResumablePreviewSummary> findResumablePreview(String triggeredByUsername) {
        Optional<MunicipalityImportRecord> previewRecord = municipalityImportRecordRepository
            .findTopByOperationTypeAndStatusAndTriggeredByUsernameOrderByCreatedAtDesc(
                MunicipalityImportOperation.PREVIEW,
                MunicipalityImportStatus.SUCCESS,
                triggeredByUsername
            );
        if (previewRecord.isEmpty()) {
            return Optional.empty();
        }

        MunicipalityImportRecord preview = previewRecord.get();
        if (preview.getCreatedAt().isBefore(OffsetDateTime.now().minus(PREVIEW_STALE_AFTER))) {
            return Optional.empty();
        }

        Optional<MunicipalityImportRecord> latestImport = municipalityImportRecordRepository
            .findTopByOperationTypeAndStatusAndTriggeredByUsernameOrderByCreatedAtDesc(
                MunicipalityImportOperation.IMPORT,
                MunicipalityImportStatus.SUCCESS,
                triggeredByUsername
            );
        if (latestImport.isPresent() && !latestImport.get().getCreatedAt().isBefore(preview.getCreatedAt())) {
            return Optional.empty();
        }

        return Optional.of(new ResumablePreviewSummary(
            preview.getId(),
            preview.getSource(),
            preview.getSourceVersion(),
            preview.getMunicipalitiesUrl(),
            preview.getPostalMappingsUrl(),
            preview.getImportedMunicipalities(),
            preview.getImportedPostalMappings(),
            preview.getCreatedAt(),
            preview.getTriggeredByUsername()
        ));
    }

    public VerificationSummary verifyDefaultSources(String triggeredByUsername) {
        return verifySources(defaultVerificationForm(), triggeredByUsername);
    }

    @Transactional(readOnly = true)
    public List<ImportHistoryItem> getRecentVerifications() {
        return municipalityImportRecordRepository.findTop50ByOperationTypeOrderByCreatedAtDesc(MunicipalityImportOperation.VERIFY).stream()
            .map(this::toHistoryItem)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ImportHistoryItem> getRecentImports() {
        return municipalityImportRecordRepository.findTop50ByOperationTypeOrderByCreatedAtDesc(MunicipalityImportOperation.IMPORT).stream()
            .map(this::toHistoryItem)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ImportHistoryItem> getRecentActivity() {
        return municipalityImportRecordRepository.findTop50ByOrderByCreatedAtDesc().stream()
            .map(this::toHistoryItem)
            .toList();
    }

    @Transactional
    public MunicipalityCatalogImportService.PreviewSummary previewImport(AdminMunicipalityImportForm form, String triggeredByUsername) {
        try {
            DownloadedCatalog catalog = downloadCatalog(form);
            MunicipalityCatalogImportService.PreviewSummary preview = municipalityCatalogImportService.previewFromResources(
                catalog.municipalitiesResource(),
                catalog.postalMappingsResource(),
                form.source().trim(),
                form.sourceVersion().trim()
            );
            municipalityImportRecordRepository.save(new MunicipalityImportRecord(
                MunicipalityImportOperation.PREVIEW,
                normalize(form.source()),
                normalize(form.sourceVersion()),
                normalize(form.municipalitiesUrl()),
                normalize(form.postalMappingsUrl()),
                MunicipalityImportStatus.SUCCESS,
                preview.municipalityRows(),
                0,
                preview.postalMappingRows(),
                0,
                triggeredByUsername,
                "Previsualización lista para importar.",
                OffsetDateTime.now()
            ));
            return preview;
        } catch (RuntimeException exception) {
            municipalityImportRecordRepository.save(new MunicipalityImportRecord(
                MunicipalityImportOperation.PREVIEW,
                normalize(form.source()),
                normalize(form.sourceVersion()),
                normalize(form.municipalitiesUrl()),
                normalize(form.postalMappingsUrl()),
                MunicipalityImportStatus.FAILED,
                0,
                0,
                0,
                0,
                triggeredByUsername,
                abbreviate(exception.getMessage()),
                OffsetDateTime.now()
            ));
            throw exception;
        }
    }

    @Transactional
    public VerificationSummary verifySources(AdminMunicipalityImportForm form, String triggeredByUsername) {
        try {
            DownloadedCatalog catalog = downloadCatalog(form);
            MunicipalityCatalogImportService.PreviewSummary preview = municipalityCatalogImportService.previewFromResources(
                catalog.municipalitiesResource(),
                catalog.postalMappingsResource(),
                form.source().trim(),
                form.sourceVersion().trim()
            );
            VerificationSummary summary = evaluateVerification(preview);
            municipalityImportRecordRepository.save(new MunicipalityImportRecord(
                MunicipalityImportOperation.VERIFY,
                normalize(form.source()),
                normalize(form.sourceVersion()),
                normalize(form.municipalitiesUrl()),
                normalize(form.postalMappingsUrl()),
                MunicipalityImportStatus.SUCCESS,
                preview.municipalityRows(),
                0,
                preview.postalMappingRows(),
                0,
                triggeredByUsername,
                summary.message(),
                OffsetDateTime.now()
            ));
            return summary;
        } catch (RuntimeException exception) {
            municipalityImportRecordRepository.save(new MunicipalityImportRecord(
                MunicipalityImportOperation.VERIFY,
                normalize(form.source()),
                normalize(form.sourceVersion()),
                normalize(form.municipalitiesUrl()),
                normalize(form.postalMappingsUrl()),
                MunicipalityImportStatus.FAILED,
                0,
                0,
                0,
                0,
                triggeredByUsername,
                abbreviate(exception.getMessage()),
                OffsetDateTime.now()
            ));
            throw exception;
        }
    }

    private AdminMunicipalityImportForm defaultVerificationForm() {
        AdminSettingsService.MunicipalityAdminDefaults defaults = adminSettingsService.getMunicipalityAdminDefaults();
        return new AdminMunicipalityImportForm(
            defaults.municipalitiesUrl(),
            defaults.postalMappingsUrl(),
            defaults.source(),
            "verify-" + OffsetDateTime.now().format(VERIFICATION_VERSION_FORMAT)
        );
    }

    private String defaultImportVersion() {
        return "manual-" + OffsetDateTime.now().format(VERIFICATION_VERSION_FORMAT);
    }

    @Transactional
    public MunicipalityCatalogImportService.ImportSummary importCatalog(AdminMunicipalityImportForm form, String triggeredByUsername) {
        try {
            DownloadedCatalog catalog = downloadCatalog(form);
            MunicipalityCatalogImportService.ImportSummary summary = municipalityCatalogImportService.importFromResources(
                catalog.municipalitiesResource(),
                catalog.postalMappingsResource(),
                form.source().trim(),
                form.sourceVersion().trim()
            );
            municipalityImportRecordRepository.save(new MunicipalityImportRecord(
                MunicipalityImportOperation.IMPORT,
                summary.source(),
                summary.sourceVersion(),
                form.municipalitiesUrl().trim(),
                form.postalMappingsUrl().trim(),
                MunicipalityImportStatus.SUCCESS,
                summary.importedMunicipalities(),
                summary.deactivatedMunicipalities(),
                summary.importedPostalMappings(),
                summary.deactivatedPostalMappings(),
                triggeredByUsername,
                null,
                OffsetDateTime.now()
            ));
            return summary;
        } catch (RuntimeException exception) {
            municipalityImportRecordRepository.save(new MunicipalityImportRecord(
                MunicipalityImportOperation.IMPORT,
                normalize(form.source()),
                normalize(form.sourceVersion()),
                normalize(form.municipalitiesUrl()),
                normalize(form.postalMappingsUrl()),
                MunicipalityImportStatus.FAILED,
                0,
                0,
                0,
                0,
                triggeredByUsername,
                abbreviate(exception.getMessage()),
                OffsetDateTime.now()
            ));
            throw exception;
        }
    }

    private DownloadedCatalog downloadCatalog(AdminMunicipalityImportForm form) {
        validateForm(form);
        DownloadedSource municipalities = download(form.municipalitiesUrl().trim(), "municipios");
        DownloadedSource postalMappings = download(form.postalMappingsUrl().trim(), "mapping postal");
        Resource municipalitiesResource = new NamedByteArrayResource(
            normalizeMunicipalities(municipalities),
            municipalities.fileName().endsWith(".csv") ? municipalities.fileName() : "spanish-municipalities.csv"
        );
        Resource postalMappingsResource = new NamedByteArrayResource(
            normalizePostalMappings(postalMappings),
            postalMappings.fileName().endsWith(".csv") ? postalMappings.fileName() : "spanish-postal-code-municipalities.csv"
        );
        return new DownloadedCatalog(municipalitiesResource, postalMappingsResource);
    }

    private void validateForm(AdminMunicipalityImportForm form) {
        if (form.municipalitiesUrl() == null || form.municipalitiesUrl().isBlank()) {
            throw new IllegalArgumentException("Indica la URL oficial del fichero de municipios.");
        }
        if (form.postalMappingsUrl() == null || form.postalMappingsUrl().isBlank()) {
            throw new IllegalArgumentException("Indica la URL del callejero oficial o del CSV postal equivalente.");
        }
        if (form.source() == null || form.source().isBlank()) {
            throw new IllegalArgumentException("Indica el origen del catálogo.");
        }
        if (form.sourceVersion() == null || form.sourceVersion().isBlank()) {
            throw new IllegalArgumentException("Indica una versión trazable de la carga.");
        }
    }

    private DownloadedSource download(String url, String label) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(DOWNLOAD_TIMEOUT)
                .GET()
                .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                throw new IllegalArgumentException("No he podido descargar el fichero de " + label + ". Código HTTP " + response.statusCode() + ".");
            }
            String fileName = resolveFileName(url, response);
            return new DownloadedSource(url, fileName, response.body(), response.headers().firstValue("Content-Type").orElse(""));
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("No he podido descargar el fichero de " + label + " desde la URL indicada.", exception);
        }
    }

    private byte[] normalizeMunicipalities(DownloadedSource source) {
        if (source.fileName().endsWith(".xlsx")) {
            return ineMunicipalityWorkbookParser.convertToInternalCsv(source.body());
        }
        if (source.fileName().endsWith(".csv")) {
            return source.body();
        }
        throw new IllegalArgumentException("El fichero de municipios debe ser un XLSX oficial del INE o un CSV ya normalizado.");
    }

    private byte[] normalizePostalMappings(DownloadedSource source) {
        if (source.fileName().endsWith(".csv")) {
            return source.body();
        }
        if (source.fileName().endsWith(".zip")) {
            Optional<byte[]> officialZip = inePostalMappingsZipParser.tryConvertToInternalCsv(source.body());
            if (officialZip.isPresent()) {
                return officialZip.get();
            }
            return unzipSingleCsv(source.body());
        }
        throw new IllegalArgumentException("El mapping postal debe ser un ZIP oficial del callejero o un CSV ya normalizado.");
    }

    private byte[] unzipSingleCsv(byte[] zipBytes) {
        try (ZipInputStream inputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            byte[] csvBytes = null;
            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".csv")) {
                    if (csvBytes != null) {
                        throw new IllegalArgumentException("El ZIP del mapping postal contiene más de un CSV. Deja uno solo.");
                    }
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    inputStream.transferTo(outputStream);
                    csvBytes = outputStream.toByteArray();
                }
            }
            if (csvBytes == null) {
                throw new IllegalArgumentException("El ZIP del mapping postal no contiene ningún CSV.");
            }
            return csvBytes;
        } catch (IOException exception) {
            throw new IllegalArgumentException("No he podido abrir el ZIP del mapping postal.", exception);
        }
    }

    private String resolveFileName(String url, HttpResponse<byte[]> response) {
        Optional<String> contentDisposition = response.headers().firstValue("Content-Disposition");
        if (contentDisposition.isPresent()) {
            String header = contentDisposition.get();
            int index = header.toLowerCase().indexOf("filename=");
            if (index >= 0) {
                String raw = header.substring(index + 9).trim().replace("\"", "");
                if (!raw.isBlank()) {
                    return raw;
                }
            }
        }
        String path = URI.create(url).getPath();
        int slashIndex = path.lastIndexOf('/');
        return slashIndex >= 0 ? path.substring(slashIndex + 1) : path;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String abbreviate(String message) {
        if (message == null || message.isBlank()) {
            return "Error de importación sin detalle adicional.";
        }
        if (message.length() <= 1000) {
            return message;
        }
        return message.substring(0, 997) + "...";
    }

    private VerificationSummary evaluateVerification(MunicipalityCatalogImportService.PreviewSummary preview) {
        List<String> warnings = new ArrayList<>();
        if (preview.municipalityRows() < MIN_REASONABLE_MUNICIPALITY_ROWS) {
            warnings.add("El fichero de municipios devuelve " + preview.municipalityRows() + " filas, muy por debajo de lo esperable para España.");
        }
        if (preview.postalMappingRows() < MIN_REASONABLE_POSTAL_MAPPING_ROWS) {
            warnings.add("El callejero solo genera " + preview.postalMappingRows() + " mappings postales, una cifra demasiado baja para una carga nacional.");
        }

        long currentMunicipalityCount = municipalityCatalogEntryRepository.countByCountryCodeAndActiveTrue(SPAIN);
        long currentPostalMappingCount = postalCodeMunicipalityMappingRepository.countByActiveTrue();

        if (currentMunicipalityCount > 0) {
            double ratio = ((double) preview.municipalityRows()) / currentMunicipalityCount;
            if (ratio < 0.8d || ratio > 1.2d) {
                warnings.add("El número de municipios difiere mucho del catálogo activo actual (" + currentMunicipalityCount + ").");
            }
        }
        if (currentPostalMappingCount > 0) {
            double ratio = ((double) preview.postalMappingRows()) / currentPostalMappingCount;
            if (ratio < 0.8d || ratio > 1.2d) {
                warnings.add("El número de mappings postales difiere mucho del catálogo activo actual (" + currentPostalMappingCount + ").");
            }
        }

        if (warnings.isEmpty()) {
            return new VerificationSummary(
                "ok",
                "Fuentes oficiales verificadas. El formato actual parece compatible con el importador.",
                preview.municipalityRows(),
                preview.postalMappingRows(),
                List.of()
            );
        }

        return new VerificationSummary(
            "warning",
            warnings.getFirst(),
            preview.municipalityRows(),
            preview.postalMappingRows(),
            warnings
        );
    }

    private SourceHealthSummary buildSourceHealth(MunicipalityImportRecord lastVerification) {
        if (lastVerification == null) {
            return new SourceHealthSummary(
                "warning",
                "Fuentes oficiales sin verificar todavía.",
                "Lanza una verificación desde esta pantalla antes de la próxima importación.",
                null
            );
        }

        if (lastVerification.getStatus() == MunicipalityImportStatus.FAILED) {
            return new SourceHealthSummary(
                "error",
                "La última verificación ha fallado.",
                lastVerification.getErrorMessage(),
                lastVerification.getCreatedAt()
            );
        }

        if (lastVerification.getCreatedAt().isBefore(OffsetDateTime.now().minus(VERIFICATION_STALE_AFTER))) {
            return new SourceHealthSummary(
                "warning",
                "La última verificación está desactualizada.",
                "Se validó el " + lastVerification.getCreatedAt().toLocalDate() + ". Conviene repetirla antes de importar.",
                lastVerification.getCreatedAt()
            );
        }

        List<String> warnings = evaluateStoredVerificationWarnings(lastVerification);
        if (!warnings.isEmpty()) {
            return new SourceHealthSummary(
                "warning",
                "La última verificación detectó avisos.",
                warnings.getFirst(),
                lastVerification.getCreatedAt()
            );
        }

        return new SourceHealthSummary(
            "ok",
            "Fuentes oficiales verificadas recientemente.",
            lastVerification.getErrorMessage(),
            lastVerification.getCreatedAt()
        );
    }

    private List<String> evaluateStoredVerificationWarnings(MunicipalityImportRecord verificationRecord) {
        List<String> warnings = new ArrayList<>();
        if (verificationRecord.getImportedMunicipalities() < MIN_REASONABLE_MUNICIPALITY_ROWS) {
            warnings.add("El fichero de municipios devolvió " + verificationRecord.getImportedMunicipalities() + " filas, muy por debajo de lo esperable para España.");
        }
        if (verificationRecord.getImportedPostalMappings() < MIN_REASONABLE_POSTAL_MAPPING_ROWS) {
            warnings.add("El callejero generó " + verificationRecord.getImportedPostalMappings() + " mappings postales, una cifra demasiado baja para una carga nacional.");
        }

        long currentMunicipalityCount = municipalityCatalogEntryRepository.countByCountryCodeAndActiveTrue(SPAIN);
        long currentPostalMappingCount = postalCodeMunicipalityMappingRepository.countByActiveTrue();
        if (currentMunicipalityCount > 0) {
            double ratio = ((double) verificationRecord.getImportedMunicipalities()) / currentMunicipalityCount;
            if (ratio < 0.8d || ratio > 1.2d) {
                warnings.add("El número de municipios difiere mucho del catálogo activo actual (" + currentMunicipalityCount + ").");
            }
        }
        if (currentPostalMappingCount > 0) {
            double ratio = ((double) verificationRecord.getImportedPostalMappings()) / currentPostalMappingCount;
            if (ratio < 0.8d || ratio > 1.2d) {
                warnings.add("El número de mappings postales difiere mucho del catálogo activo actual (" + currentPostalMappingCount + ").");
            }
        }
        return warnings;
    }

    private ImportHistoryItem toHistoryItem(MunicipalityImportRecord record) {
        return new ImportHistoryItem(
            record.getId(),
            record.getOperationType().name(),
            record.getSource(),
            record.getSourceVersion(),
            record.getStatus().name(),
            record.getImportedMunicipalities(),
            record.getImportedPostalMappings(),
            record.getCreatedAt(),
            record.getTriggeredByUsername(),
            record.getErrorMessage()
        );
    }

    public record DashboardSummary(
        boolean catalogLoaded,
        long activeMunicipalityCount,
        long activePostalMappingCount,
        String currentSource,
        String currentSourceVersion,
        OffsetDateTime currentUpdatedAt,
        SourceHealthSummary sourceHealth,
        ImportHistoryItem lastImport,
        List<ImportHistoryItem> recentImports
    ) {
    }

    public record SourceHealthSummary(
        String level,
        String title,
        String detail,
        OffsetDateTime checkedAt
    ) {
    }

    public record ImportHistoryItem(
        Long id,
        String operationType,
        String source,
        String sourceVersion,
        String status,
        int importedMunicipalities,
        int importedPostalMappings,
        OffsetDateTime createdAt,
        String triggeredByUsername,
        String errorMessage
    ) {
    }

    public record ResumablePreviewSummary(
        Long id,
        String source,
        String sourceVersion,
        String municipalitiesUrl,
        String postalMappingsUrl,
        int municipalityRows,
        int postalMappingRows,
        OffsetDateTime createdAt,
        String triggeredByUsername
    ) {
        public AdminMunicipalityImportForm toForm() {
            return new AdminMunicipalityImportForm(
                municipalitiesUrl,
                postalMappingsUrl,
                source,
                sourceVersion
            );
        }
    }

    public record VerificationSummary(
        String level,
        String message,
        int municipalityRows,
        int postalMappingRows,
        List<String> warnings
    ) {
    }

    private record DownloadedCatalog(Resource municipalitiesResource, Resource postalMappingsResource) {
    }

    private record DownloadedSource(String url, String fileName, byte[] body, String contentType) {
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public String getDescription() {
            return filename;
        }
    }
}
