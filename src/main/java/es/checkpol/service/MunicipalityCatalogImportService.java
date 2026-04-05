package es.checkpol.service;

import es.checkpol.domain.MunicipalityCatalogEntry;
import es.checkpol.domain.PostalCodeMunicipalityMapping;
import es.checkpol.repository.MunicipalityCatalogEntryRepository;
import es.checkpol.repository.PostalCodeMunicipalityMappingRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class MunicipalityCatalogImportService {

    public static final String DEFAULT_SOURCE = "classpath-csv";
    public static final String DEFAULT_SOURCE_VERSION = "example-v1";
    private static final Pattern PROVINCE_CODE_PATTERN = Pattern.compile("^\\d{2}$");
    private static final Pattern MUNICIPALITY_CODE_PATTERN = Pattern.compile("^\\d{5}$");
    private static final Pattern SPANISH_POSTAL_CODE_PATTERN = Pattern.compile("^\\d{5}$");
    private static final List<String> MUNICIPALITY_HEADERS = List.of("provinceCode", "provinceName", "municipalityCode", "municipalityName");
    private static final List<String> POSTAL_MAPPING_HEADERS = List.of("postalCode", "municipalityCode");

    private final MunicipalityCatalogEntryRepository municipalityCatalogEntryRepository;
    private final PostalCodeMunicipalityMappingRepository postalCodeMunicipalityMappingRepository;

    public MunicipalityCatalogImportService(
        MunicipalityCatalogEntryRepository municipalityCatalogEntryRepository,
        PostalCodeMunicipalityMappingRepository postalCodeMunicipalityMappingRepository
    ) {
        this.municipalityCatalogEntryRepository = municipalityCatalogEntryRepository;
        this.postalCodeMunicipalityMappingRepository = postalCodeMunicipalityMappingRepository;
    }

    @Transactional
    public ImportSummary importFromResources(
        Resource municipalitiesResource,
        Resource postalMappingsResource,
        String source,
        String sourceVersion
    ) {
        ParsedCatalog catalog = loadCatalog(municipalitiesResource, postalMappingsResource, source, sourceVersion);
        return importCatalog(catalog);
    }

    @Transactional(readOnly = true)
    public PreviewSummary previewFromResources(
        Resource municipalitiesResource,
        Resource postalMappingsResource,
        String source,
        String sourceVersion
    ) {
        ParsedCatalog catalog = loadCatalog(municipalitiesResource, postalMappingsResource, source, sourceVersion);
        return previewCatalog(catalog);
    }

    private ParsedCatalog loadCatalog(
        Resource municipalitiesResource,
        Resource postalMappingsResource,
        String source,
        String sourceVersion
    ) {
        validateSourceMetadata(source, sourceVersion);
        List<MunicipalityRow> municipalityRows = parseMunicipalityRows(municipalitiesResource);
        List<PostalMappingRow> postalMappingRows = parsePostalMappingRows(postalMappingsResource);
        return new ParsedCatalog(municipalityRows, postalMappingRows, source, sourceVersion);
    }

    private ImportSummary importCatalog(ParsedCatalog catalog) {
        OffsetDateTime now = OffsetDateTime.now();
        Map<String, MunicipalityCatalogEntry> existingMunicipalities = existingMunicipalities();
        Set<String> seenMunicipalityCodes = validateMunicipalityRows(catalog.municipalityRows());

        int importedMunicipalities = 0;
        for (MunicipalityRow row : catalog.municipalityRows()) {
            MunicipalityCatalogEntry entry = existingMunicipalities.get(row.municipalityCode());
            if (entry == null) {
                municipalityCatalogEntryRepository.save(new MunicipalityCatalogEntry(
                    "ESP",
                    row.provinceCode(),
                    row.provinceName(),
                    row.municipalityCode(),
                    row.municipalityName(),
                    MunicipalityTextNormalizer.normalizeText(row.municipalityName()),
                    true,
                    catalog.source(),
                    catalog.sourceVersion(),
                    now,
                    now
                ));
            } else {
                entry.refreshFromImport(
                    row.provinceCode(),
                    row.provinceName(),
                    row.municipalityName(),
                    MunicipalityTextNormalizer.normalizeText(row.municipalityName()),
                    true,
                    catalog.source(),
                    catalog.sourceVersion(),
                    now
                );
            }
            importedMunicipalities++;
        }

        int deactivatedMunicipalities = 0;
        for (MunicipalityCatalogEntry entry : existingMunicipalities.values()) {
            if (catalog.source().equals(entry.getSource()) && !seenMunicipalityCodes.contains(entry.getMunicipalityCode()) && entry.isActive()) {
                entry.deactivate(catalog.source(), catalog.sourceVersion(), now);
                deactivatedMunicipalities++;
            }
        }

        Map<String, PostalCodeMunicipalityMapping> existingMappings = existingMappings();
        Set<String> seenMappings = validatePostalMappings(catalog.postalMappingRows(), seenMunicipalityCodes);

        int importedMappings = 0;
        for (PostalMappingRow row : catalog.postalMappingRows()) {
            String key = row.postalCode() + "|" + row.municipalityCode();
            PostalCodeMunicipalityMapping mapping = existingMappings.get(key);
            if (mapping == null) {
                postalCodeMunicipalityMappingRepository.save(new PostalCodeMunicipalityMapping(
                    row.postalCode(),
                    row.municipalityCode(),
                    true,
                    catalog.source(),
                    catalog.sourceVersion(),
                    now,
                    now
                ));
            } else {
                mapping.refreshFromImport(true, catalog.source(), catalog.sourceVersion(), now);
            }
            importedMappings++;
        }

        int deactivatedMappings = 0;
        for (PostalCodeMunicipalityMapping mapping : existingMappings.values()) {
            String key = mapping.getPostalCode() + "|" + mapping.getMunicipalityCode();
            if (catalog.source().equals(mapping.getSource()) && !seenMappings.contains(key) && mapping.isActive()) {
                mapping.deactivate(catalog.source(), catalog.sourceVersion(), now);
                deactivatedMappings++;
            }
        }

        return new ImportSummary(
            importedMunicipalities,
            deactivatedMunicipalities,
            importedMappings,
            deactivatedMappings,
            catalog.source(),
            catalog.sourceVersion()
        );
    }

    private PreviewSummary previewCatalog(ParsedCatalog catalog) {
        Map<String, MunicipalityCatalogEntry> existingMunicipalities = existingMunicipalities();
        Set<String> seenMunicipalityCodes = validateMunicipalityRows(catalog.municipalityRows());
        Map<String, PostalCodeMunicipalityMapping> existingMappings = existingMappings();
        Set<String> seenMappings = validatePostalMappings(catalog.postalMappingRows(), seenMunicipalityCodes);

        int newMunicipalities = 0;
        int updatedMunicipalities = 0;
        for (MunicipalityRow row : catalog.municipalityRows()) {
            MunicipalityCatalogEntry existing = existingMunicipalities.get(row.municipalityCode());
            if (existing == null) {
                newMunicipalities++;
                continue;
            }
            if (!existing.isActive()
                || !row.provinceCode().equals(existing.getProvinceCode())
                || !row.provinceName().equals(existing.getProvinceName())
                || !row.municipalityName().equals(existing.getMunicipalityName())) {
                updatedMunicipalities++;
            }
        }

        int deactivatedMunicipalities = 0;
        for (MunicipalityCatalogEntry entry : existingMunicipalities.values()) {
            if (catalog.source().equals(entry.getSource()) && !seenMunicipalityCodes.contains(entry.getMunicipalityCode()) && entry.isActive()) {
                deactivatedMunicipalities++;
            }
        }

        int newMappings = 0;
        int reactivatedMappings = 0;
        for (PostalMappingRow row : catalog.postalMappingRows()) {
            String key = row.postalCode() + "|" + row.municipalityCode();
            PostalCodeMunicipalityMapping existing = existingMappings.get(key);
            if (existing == null) {
                newMappings++;
                continue;
            }
            if (!existing.isActive()) {
                reactivatedMappings++;
            }
        }

        int deactivatedMappings = 0;
        for (PostalCodeMunicipalityMapping mapping : existingMappings.values()) {
            String key = mapping.getPostalCode() + "|" + mapping.getMunicipalityCode();
            if (catalog.source().equals(mapping.getSource()) && !seenMappings.contains(key) && mapping.isActive()) {
                deactivatedMappings++;
            }
        }

        return new PreviewSummary(
            catalog.municipalityRows().size(),
            catalog.postalMappingRows().size(),
            newMunicipalities,
            updatedMunicipalities,
            deactivatedMunicipalities,
            newMappings,
            reactivatedMappings,
            deactivatedMappings,
            catalog.source(),
            catalog.sourceVersion(),
            catalog.municipalityRows().stream().limit(5).toList(),
            catalog.postalMappingRows().stream().limit(5).toList()
        );
    }

    private Map<String, MunicipalityCatalogEntry> existingMunicipalities() {
        Map<String, MunicipalityCatalogEntry> existingMunicipalities = new LinkedHashMap<>();
        for (MunicipalityCatalogEntry entry : municipalityCatalogEntryRepository.findAll()) {
            existingMunicipalities.put(entry.getMunicipalityCode(), entry);
        }
        return existingMunicipalities;
    }

    private Map<String, PostalCodeMunicipalityMapping> existingMappings() {
        Map<String, PostalCodeMunicipalityMapping> existingMappings = new LinkedHashMap<>();
        for (PostalCodeMunicipalityMapping mapping : postalCodeMunicipalityMappingRepository.findAll()) {
            existingMappings.put(mapping.getPostalCode() + "|" + mapping.getMunicipalityCode(), mapping);
        }
        return existingMappings;
    }

    private Set<String> validateMunicipalityRows(List<MunicipalityRow> municipalityRows) {
        Set<String> seenMunicipalityCodes = new LinkedHashSet<>();
        for (MunicipalityRow row : municipalityRows) {
            if (!seenMunicipalityCodes.add(row.municipalityCode())) {
                throw new IllegalArgumentException("El CSV de municipios contiene un código repetido: " + row.municipalityCode());
            }
        }
        return seenMunicipalityCodes;
    }

    private Set<String> validatePostalMappings(List<PostalMappingRow> postalMappingRows, Set<String> validMunicipalityCodes) {
        Set<String> seenMappings = new LinkedHashSet<>();
        for (PostalMappingRow row : postalMappingRows) {
            if (!validMunicipalityCodes.contains(row.municipalityCode())) {
                throw new IllegalArgumentException("El mapping postal hace referencia a un municipio no cargado: " + row.municipalityCode());
            }
            String key = row.postalCode() + "|" + row.municipalityCode();
            if (!seenMappings.add(key)) {
                throw new IllegalArgumentException("El CSV postal contiene un mapping repetido: " + key);
            }
        }
        return seenMappings;
    }

    private List<MunicipalityRow> parseMunicipalityRows(Resource resource) {
        List<Map<String, String>> rows = parseRows(resource, MUNICIPALITY_HEADERS);
        List<MunicipalityRow> result = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String provinceCode = required(row, "provinceCode");
            String provinceName = required(row, "provinceName");
            String municipalityCode = required(row, "municipalityCode");
            String municipalityName = required(row, "municipalityName");
            if (!PROVINCE_CODE_PATTERN.matcher(provinceCode).matches()) {
                throw new IllegalArgumentException("provinceCode debe tener 2 dígitos: " + provinceCode);
            }
            if (!MUNICIPALITY_CODE_PATTERN.matcher(municipalityCode).matches()) {
                throw new IllegalArgumentException("municipalityCode debe tener 5 dígitos: " + municipalityCode);
            }
            result.add(new MunicipalityRow(provinceCode, provinceName, municipalityCode, municipalityName));
        }
        return result;
    }

    private List<PostalMappingRow> parsePostalMappingRows(Resource resource) {
        List<Map<String, String>> rows = parseRows(resource, POSTAL_MAPPING_HEADERS);
        List<PostalMappingRow> result = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String postalCode = required(row, "postalCode");
            String municipalityCode = required(row, "municipalityCode");
            if (!SPANISH_POSTAL_CODE_PATTERN.matcher(postalCode).matches()) {
                throw new IllegalArgumentException("postalCode debe tener 5 dígitos: " + postalCode);
            }
            if (!MUNICIPALITY_CODE_PATTERN.matcher(municipalityCode).matches()) {
                throw new IllegalArgumentException("municipalityCode debe tener 5 dígitos: " + municipalityCode);
            }
            result.add(new PostalMappingRow(postalCode, municipalityCode));
        }
        return result;
    }

    private List<Map<String, String>> parseRows(Resource resource, List<String> expectedHeaders) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> headers = null;
            List<Map<String, String>> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                List<String> columns = split(trimmed);
                if (headers == null) {
                    headers = columns;
                    if (!headers.equals(expectedHeaders)) {
                        throw new IllegalArgumentException("Cabecera CSV inválida en " + resource.getFilename() + ". Esperaba " + expectedHeaders);
                    }
                    continue;
                }
                if (columns.size() != headers.size()) {
                    throw new IllegalArgumentException("Fila CSV inválida en " + resource.getFilename() + ": " + trimmed);
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (int index = 0; index < headers.size(); index++) {
                    row.put(headers.get(index), columns.get(index));
                }
                rows.add(row);
            }
            if (headers == null) {
                throw new IllegalArgumentException("El CSV " + resource.getFilename() + " no contiene cabecera.");
            }
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("El CSV " + resource.getFilename() + " no contiene filas de datos.");
            }
            return rows;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("No he podido importar el catálogo de municipios desde " + resource.getDescription(), exception);
        }
    }

    private void validateSourceMetadata(String source, String sourceVersion) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("El origen del catálogo no puede estar vacío.");
        }
        if (sourceVersion == null || sourceVersion.isBlank()) {
            throw new IllegalArgumentException("La versión del catálogo no puede estar vacía.");
        }
    }

    private List<String> split(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);
            if (currentChar == '"') {
                quoted = !quoted;
                continue;
            }
            if (currentChar == ';' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(currentChar);
        }
        values.add(current.toString().trim());
        return values;
    }

    private String required(Map<String, String> row, String field) {
        String value = row.get(field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta el campo " + field + " en el CSV de municipios.");
        }
        return value.trim();
    }

    public record MunicipalityRow(
        String provinceCode,
        String provinceName,
        String municipalityCode,
        String municipalityName
    ) {
    }

    public record PostalMappingRow(
        String postalCode,
        String municipalityCode
    ) {
    }

    public record ImportSummary(
        int importedMunicipalities,
        int deactivatedMunicipalities,
        int importedPostalMappings,
        int deactivatedPostalMappings,
        String source,
        String sourceVersion
    ) {
    }

    public record PreviewSummary(
        int municipalityRows,
        int postalMappingRows,
        int newMunicipalities,
        int updatedMunicipalities,
        int deactivatedMunicipalities,
        int newPostalMappings,
        int reactivatedPostalMappings,
        int deactivatedPostalMappings,
        String source,
        String sourceVersion,
        List<MunicipalityRow> municipalitySamples,
        List<PostalMappingRow> postalMappingSamples
    ) {
    }

    private record ParsedCatalog(
        List<MunicipalityRow> municipalityRows,
        List<PostalMappingRow> postalMappingRows,
        String source,
        String sourceVersion
    ) {
    }
}
