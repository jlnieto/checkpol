package es.checkpol.service;

import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class IneMunicipalityWorkbookParser {

    private static final List<String> EXPECTED_HEADERS = List.of("CODAUTO", "CPRO", "CMUN", "DC", "NOMBRE");

    public byte[] convertToInternalCsv(byte[] workbookBytes) {
        Map<String, byte[]> entries = unzip(workbookBytes);
        List<String> sharedStrings = parseSharedStrings(entries.get("xl/sharedStrings.xml"));
        List<Map<String, String>> rows = parseRows(entries.get("xl/worksheets/sheet1.xml"), sharedStrings);
        validateHeaders(rows);

        StringBuilder csv = new StringBuilder();
        csv.append("provinceCode;provinceName;municipalityCode;municipalityName\n");
        for (int index = 2; index < rows.size(); index++) {
            Map<String, String> row = rows.get(index);
            String provinceCode = leftPad(row.getOrDefault("B", ""), 2);
            String municipalityCode = provinceCode + leftPad(row.getOrDefault("C", ""), 3);
            String municipalityName = row.getOrDefault("E", "").trim();
            if (provinceCode.isBlank() && municipalityName.isBlank()) {
                continue;
            }
            SpanishProvinceDirectory.ProvinceInfo province = SpanishProvinceDirectory.findByCode(provinceCode);
            if (province == null) {
                throw new IllegalArgumentException("No he reconocido la provincia del fichero INE: " + provinceCode);
            }
            if (municipalityName.isBlank()) {
                throw new IllegalArgumentException("El fichero INE contiene un municipio sin nombre para el código " + municipalityCode + ".");
            }
            csv.append(provinceCode)
                .append(';')
                .append(escape(province.queryName()))
                .append(';')
                .append(municipalityCode)
                .append(';')
                .append(escape(municipalityName))
                .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, byte[]> unzip(byte[] workbookBytes) {
        try (ZipInputStream inputStream = new ZipInputStream(new ByteArrayInputStream(workbookBytes))) {
            Map<String, byte[]> entries = new LinkedHashMap<>();
            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                inputStream.transferTo(outputStream);
                entries.put(entry.getName(), outputStream.toByteArray());
            }
            return entries;
        } catch (Exception exception) {
            throw new IllegalArgumentException("No he podido abrir el XLSX oficial del INE.", exception);
        }
    }

    private List<String> parseSharedStrings(byte[] xmlBytes) {
        if (xmlBytes == null) {
            return List.of();
        }
        org.w3c.dom.Document document = parseXml(xmlBytes);
        org.w3c.dom.NodeList nodes = document.getElementsByTagNameNS("*", "si");
        List<String> sharedStrings = new ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            org.w3c.dom.Element node = (org.w3c.dom.Element) nodes.item(index);
            org.w3c.dom.NodeList textNodes = node.getElementsByTagNameNS("*", "t");
            StringBuilder value = new StringBuilder();
            for (int textIndex = 0; textIndex < textNodes.getLength(); textIndex++) {
                value.append(textNodes.item(textIndex).getTextContent());
            }
            sharedStrings.add(value.toString());
        }
        return sharedStrings;
    }

    private List<Map<String, String>> parseRows(byte[] sheetBytes, List<String> sharedStrings) {
        if (sheetBytes == null) {
            throw new IllegalArgumentException("El XLSX oficial del INE no contiene la hoja esperada.");
        }
        org.w3c.dom.Document document = parseXml(sheetBytes);
        org.w3c.dom.NodeList rowNodes = document.getElementsByTagNameNS("*", "row");
        List<Map<String, String>> rows = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rowNodes.getLength(); rowIndex++) {
            org.w3c.dom.Element rowElement = (org.w3c.dom.Element) rowNodes.item(rowIndex);
            org.w3c.dom.NodeList cellNodes = rowElement.getElementsByTagNameNS("*", "c");
            Map<String, String> valuesByColumn = new LinkedHashMap<>();
            for (int cellIndex = 0; cellIndex < cellNodes.getLength(); cellIndex++) {
                org.w3c.dom.Element cell = (org.w3c.dom.Element) cellNodes.item(cellIndex);
                String reference = cell.getAttribute("r");
                String column = reference.replaceAll("\\d", "");
                String value = "";
                org.w3c.dom.NodeList valueNodes = cell.getElementsByTagNameNS("*", "v");
                if (valueNodes.getLength() > 0) {
                    value = valueNodes.item(0).getTextContent();
                    if ("s".equals(cell.getAttribute("t"))) {
                        value = sharedStrings.get(Integer.parseInt(value));
                    }
                }
                valuesByColumn.put(column, value);
            }
            rows.add(valuesByColumn);
        }
        return rows;
    }

    private void validateHeaders(List<Map<String, String>> rows) {
        if (rows.size() < 2) {
            throw new IllegalArgumentException("El XLSX oficial del INE no contiene filas suficientes.");
        }
        Map<String, String> headerRow = rows.get(1);
        List<String> actualHeaders = List.of(
            headerRow.getOrDefault("A", ""),
            headerRow.getOrDefault("B", ""),
            headerRow.getOrDefault("C", ""),
            headerRow.getOrDefault("D", ""),
            headerRow.getOrDefault("E", "")
        );
        if (!EXPECTED_HEADERS.equals(actualHeaders)) {
            throw new IllegalArgumentException("La cabecera del XLSX oficial del INE no coincide con el formato esperado.");
        }
    }

    private org.w3c.dom.Document parseXml(byte[] xmlBytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes));
        } catch (Exception exception) {
            throw new IllegalArgumentException("No he podido leer el XLSX oficial del INE.", exception);
        }
    }

    private String leftPad(String value, int size) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.length() >= size) {
            return trimmed;
        }
        return "0".repeat(size - trimmed.length()) + trimmed;
    }

    private String escape(String value) {
        String sanitized = value == null ? "" : value.trim();
        if (!sanitized.contains(";") && !sanitized.contains("\"")) {
            return sanitized;
        }
        return "\"" + sanitized.replace("\"", "\"\"") + "\"";
    }
}
