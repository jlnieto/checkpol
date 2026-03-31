package es.checkpol.service;

import es.checkpol.domain.MunicipalityCatalogEntry;
import es.checkpol.domain.MunicipalityImportRecord;
import es.checkpol.domain.MunicipalityImportStatus;
import es.checkpol.repository.MunicipalityCatalogEntryRepository;
import es.checkpol.repository.MunicipalityImportRecordRepository;
import es.checkpol.repository.PostalCodeMunicipalityMappingRepository;
import es.checkpol.web.AdminMunicipalityImportForm;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class MunicipalityAdminService {

    private static final String SPAIN = "ESP";
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(3);

    private final MunicipalityCatalogService municipalityCatalogService;
    private final MunicipalityCatalogImportService municipalityCatalogImportService;
    private final MunicipalityCatalogEntryRepository municipalityCatalogEntryRepository;
    private final PostalCodeMunicipalityMappingRepository postalCodeMunicipalityMappingRepository;
    private final MunicipalityImportRecordRepository municipalityImportRecordRepository;
    private final IneMunicipalityWorkbookParser ineMunicipalityWorkbookParser;
    private final InePostalMappingsZipParser inePostalMappingsZipParser;
    private final HttpClient httpClient;
    private final String defaultMunicipalitiesUrl;
    private final String defaultPostalMappingsUrl;
    private final String defaultSource;

    public MunicipalityAdminService(
        MunicipalityCatalogService municipalityCatalogService,
        MunicipalityCatalogImportService municipalityCatalogImportService,
        MunicipalityCatalogEntryRepository municipalityCatalogEntryRepository,
        PostalCodeMunicipalityMappingRepository postalCodeMunicipalityMappingRepository,
        MunicipalityImportRecordRepository municipalityImportRecordRepository,
        IneMunicipalityWorkbookParser ineMunicipalityWorkbookParser,
        InePostalMappingsZipParser inePostalMappingsZipParser,
        @Value("${checkpol.municipality.admin.default-municipalities-url:https://www.ine.es/daco/daco42/codmun/diccionario26.xlsx}") String defaultMunicipalitiesUrl,
        @Value("${checkpol.municipality.admin.default-postal-mappings-url:https://www.ine.es/prodyser/callejero/caj_esp/caj_esp_072025.zip}") String defaultPostalMappingsUrl,
        @Value("${checkpol.municipality.admin.default-source:ine-open-data}") String defaultSource
    ) {
        this.municipalityCatalogService = municipalityCatalogService;
        this.municipalityCatalogImportService = municipalityCatalogImportService;
        this.municipalityCatalogEntryRepository = municipalityCatalogEntryRepository;
        this.postalCodeMunicipalityMappingRepository = postalCodeMunicipalityMappingRepository;
        this.municipalityImportRecordRepository = municipalityImportRecordRepository;
        this.ineMunicipalityWorkbookParser = ineMunicipalityWorkbookParser;
        this.inePostalMappingsZipParser = inePostalMappingsZipParser;
        this.defaultMunicipalitiesUrl = defaultMunicipalitiesUrl;
        this.defaultPostalMappingsUrl = defaultPostalMappingsUrl;
        this.defaultSource = defaultSource;
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(DOWNLOAD_TIMEOUT)
            .build();
    }

    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary() {
        Optional<MunicipalityCatalogEntry> currentDataset = municipalityCatalogEntryRepository.findTopByCountryCodeAndActiveTrueOrderByUpdatedAtDesc(SPAIN);
        Optional<MunicipalityImportRecord> lastImport = municipalityImportRecordRepository.findTop10ByOrderByCreatedAtDesc().stream().findFirst();
        return new DashboardSummary(
            municipalityCatalogService.hasSpanishCatalogData(),
            municipalityCatalogEntryRepository.countByCountryCodeAndActiveTrue(SPAIN),
            postalCodeMunicipalityMappingRepository.countByActiveTrue(),
            currentDataset.map(MunicipalityCatalogEntry::getSource).orElse(null),
            currentDataset.map(MunicipalityCatalogEntry::getSourceVersion).orElse(null),
            currentDataset.map(MunicipalityCatalogEntry::getUpdatedAt).orElse(null),
            lastImport.map(record -> new ImportHistoryItem(
                record.getId(),
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
        return new AdminMunicipalityImportForm(
            defaultMunicipalitiesUrl,
            defaultPostalMappingsUrl,
            defaultSource,
            ""
        );
    }

    @Transactional(readOnly = true)
    public MunicipalityCatalogImportService.PreviewSummary previewImport(AdminMunicipalityImportForm form) {
        DownloadedCatalog catalog = downloadCatalog(form);
        return municipalityCatalogImportService.previewFromResources(
            catalog.municipalitiesResource(),
            catalog.postalMappingsResource(),
            form.source().trim(),
            form.sourceVersion().trim()
        );
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

    public record DashboardSummary(
        boolean catalogLoaded,
        long activeMunicipalityCount,
        long activePostalMappingCount,
        String currentSource,
        String currentSourceVersion,
        OffsetDateTime currentUpdatedAt,
        ImportHistoryItem lastImport,
        List<ImportHistoryItem> recentImports
    ) {
    }

    public record ImportHistoryItem(
        Long id,
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
