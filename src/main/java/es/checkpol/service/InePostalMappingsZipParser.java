package es.checkpol.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class InePostalMappingsZipParser {

    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{5}$");
    private static final int MUNICIPALITY_CODE_START = 0;
    private static final int MUNICIPALITY_CODE_END = 5;
    private static final int POSTAL_CODE_START = 42;
    private static final int POSTAL_CODE_END = 47;

    public Optional<byte[]> tryConvertToInternalCsv(byte[] zipBytes) {
        try (ZipInputStream inputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.ISO_8859_1)) {
            Set<String> mappings = new LinkedHashSet<>();
            boolean tramEntryFound = false;

            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                if (entry.isDirectory() || !isTramEntry(entry.getName())) {
                    continue;
                }
                tramEntryFound = true;
                parseTramEntry(inputStream, mappings, entry.getName());
            }

            if (!tramEntryFound) {
                return Optional.empty();
            }
            if (mappings.isEmpty()) {
                throw new IllegalArgumentException("El ZIP oficial del callejero no contiene ningún mapping postal utilizable.");
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write("postalCode;municipalityCode\n".getBytes(StandardCharsets.UTF_8));
            for (String mapping : mappings) {
                outputStream.write(mapping.getBytes(StandardCharsets.UTF_8));
                outputStream.write('\n');
            }
            return Optional.of(outputStream.toByteArray());
        } catch (IOException exception) {
            throw new IllegalArgumentException("No he podido leer el ZIP oficial del callejero.", exception);
        }
    }

    private void parseTramEntry(ZipInputStream inputStream, Set<String> mappings, String entryName) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1));
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            if (line.length() < POSTAL_CODE_END) {
                throw invalidFormat(entryName, lineNumber, "Longitud de registro insuficiente.");
            }

            String municipalityCode = line.substring(MUNICIPALITY_CODE_START, MUNICIPALITY_CODE_END).trim();
            String postalCode = line.substring(POSTAL_CODE_START, POSTAL_CODE_END).trim();

            if (!CODE_PATTERN.matcher(municipalityCode).matches()) {
                throw invalidFormat(entryName, lineNumber, "No he encontrado un código de municipio válido en la cabecera del tramo.");
            }
            if ("00000".equals(postalCode) || postalCode.isBlank()) {
                continue;
            }
            if (!CODE_PATTERN.matcher(postalCode).matches()) {
                throw invalidFormat(entryName, lineNumber, "No he encontrado un código postal válido en el tramo.");
            }

            mappings.add(postalCode + ';' + municipalityCode);
        }
    }

    private boolean isTramEntry(String entryName) {
        String normalized = entryName.replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        String fileName = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
        return fileName.startsWith("TRAM.");
    }

    private IllegalArgumentException invalidFormat(String entryName, int lineNumber, String detail) {
        return new IllegalArgumentException(
            "El fichero " + entryName + " no tiene el formato esperado del callejero oficial. Línea " + lineNumber + ". " + detail
        );
    }
}
