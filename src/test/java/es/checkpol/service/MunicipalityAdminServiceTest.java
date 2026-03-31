package es.checkpol.service;

import com.sun.net.httpserver.HttpServer;
import es.checkpol.repository.MunicipalityCatalogEntryRepository;
import es.checkpol.repository.MunicipalityImportRecordRepository;
import es.checkpol.repository.PostalCodeMunicipalityMappingRepository;
import es.checkpol.web.AdminMunicipalityImportForm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "checkpol.municipality.catalog.import-on-startup=false")
@ActiveProfiles("test")
@Transactional
class MunicipalityAdminServiceTest {

    @Autowired
    private MunicipalityAdminService municipalityAdminService;

    @Autowired
    private MunicipalityCatalogEntryRepository municipalityCatalogEntryRepository;

    @Autowired
    private PostalCodeMunicipalityMappingRepository postalCodeMunicipalityMappingRepository;

    @Autowired
    private MunicipalityImportRecordRepository municipalityImportRecordRepository;

    private HttpServer httpServer;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.start();
        baseUrl = "http://127.0.0.1:" + httpServer.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void previewsAndImportsRemoteCatalogFromOfficialMunicipalitiesAndPostalZip() {
        httpServer.createContext("/municipalities.xlsx", exchange -> respond(exchange, xlsxWorkbook()));
        httpServer.createContext("/callejero.zip", exchange -> respond(exchange, postalZip()));

        AdminMunicipalityImportForm form = new AdminMunicipalityImportForm(
            baseUrl + "/municipalities.xlsx",
            baseUrl + "/callejero.zip",
            "ine-open-data",
            "2026-01"
        );

        MunicipalityCatalogImportService.PreviewSummary preview = municipalityAdminService.previewImport(form);
        assertEquals(2, preview.municipalityRows());
        assertEquals(2, preview.postalMappingRows());
        assertEquals(2, preview.newMunicipalities());
        assertEquals(2, preview.newPostalMappings());

        MunicipalityCatalogImportService.ImportSummary summary = municipalityAdminService.importCatalog(form, "admin");
        assertEquals(2, summary.importedMunicipalities());
        assertEquals(2, summary.importedPostalMappings());
        assertEquals(2, municipalityCatalogEntryRepository.countByCountryCodeAndActiveTrue("ESP"));
        assertEquals(2, postalCodeMunicipalityMappingRepository.countByActiveTrue());
        assertEquals(1, municipalityImportRecordRepository.findTop10ByOrderByCreatedAtDesc().size());
    }

    @Test
    void keepsSupportingNormalizedPostalCsvAsFallback() {
        httpServer.createContext("/municipalities.xlsx", exchange -> respond(exchange, xlsxWorkbook()));
        httpServer.createContext("/postal.csv", exchange -> respond(exchange, """
            postalCode;municipalityCode
            28001;28079
            35540;35014
            """.getBytes(StandardCharsets.UTF_8)));

        AdminMunicipalityImportForm form = new AdminMunicipalityImportForm(
            baseUrl + "/municipalities.xlsx",
            baseUrl + "/postal.csv",
            "ine-open-data",
            "2026-01"
        );

        MunicipalityCatalogImportService.PreviewSummary preview = municipalityAdminService.previewImport(form);

        assertEquals(2, preview.municipalityRows());
        assertEquals(2, preview.postalMappingRows());
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, byte[] body) throws java.io.IOException {
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    private byte[] xlsxWorkbook() {
        String sharedStringsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="15" uniqueCount="15">
              <si><t>Relación de municipios y códigos por comunidades autónomas y provincias a 1 de enero de 2026</t></si>
              <si><t>CODAUTO</t></si>
              <si><t>CPRO</t></si>
              <si><t>CMUN</t></si>
              <si><t>DC</t></si>
              <si><t>NOMBRE</t></si>
              <si><t>13</t></si>
              <si><t>28</t></si>
              <si><t>079</t></si>
              <si><t>0</t></si>
              <si><t>Madrid</t></si>
              <si><t>05</t></si>
              <si><t>35</t></si>
              <si><t>014</t></si>
              <si><t>La Oliva</t></si>
            </sst>
            """;

        String sheetXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
                <row r="1">
                  <c r="A1" t="s"><v>0</v></c>
                </row>
                <row r="2">
                  <c r="A2" t="s"><v>1</v></c>
                  <c r="B2" t="s"><v>2</v></c>
                  <c r="C2" t="s"><v>3</v></c>
                  <c r="D2" t="s"><v>4</v></c>
                  <c r="E2" t="s"><v>5</v></c>
                </row>
                <row r="3">
                  <c r="A3" t="s"><v>6</v></c>
                  <c r="B3" t="s"><v>7</v></c>
                  <c r="C3" t="s"><v>8</v></c>
                  <c r="D3" t="s"><v>9</v></c>
                  <c r="E3" t="s"><v>10</v></c>
                </row>
                <row r="4">
                  <c r="A4" t="s"><v>11</v></c>
                  <c r="B4" t="s"><v>12</v></c>
                  <c r="C4" t="s"><v>13</v></c>
                  <c r="D4" t="s"><v>9</v></c>
                  <c r="E4" t="s"><v>14</v></c>
                </row>
              </sheetData>
            </worksheet>
            """;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
                addZipEntry(zipOutputStream, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                      <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
                    </Types>
                    """);
                addZipEntry(zipOutputStream, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                    """);
                addZipEntry(zipOutputStream, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                              xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets>
                        <sheet name="Sheet1" sheetId="1" r:id="rId1"/>
                      </sheets>
                    </workbook>
                    """);
                addZipEntry(zipOutputStream, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                      <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
                    </Relationships>
                    """);
                addZipEntry(zipOutputStream, "xl/sharedStrings.xml", sharedStringsXml);
                addZipEntry(zipOutputStream, "xl/worksheets/sheet1.xml", sheetXml);
            }
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private byte[] postalZip() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.ISO_8859_1)) {
                addZipEntry(zipOutputStream, "caj_esp_072025/TRAM.D250630.G250702",
                    tramLine("28079", "28001") + "\n" + tramLine("35014", "35540") + "\n");
            }
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String tramLine(String municipalityCode, String postalCode) {
        char[] buffer = new char[280];
        java.util.Arrays.fill(buffer, ' ');
        write(buffer, 0, municipalityCode);
        write(buffer, 42, postalCode);
        write(buffer, 69, municipalityCode);
        return new String(buffer);
    }

    private void write(char[] buffer, int start, String value) {
        for (int index = 0; index < value.length(); index++) {
            buffer[start + index] = value.charAt(index);
        }
    }

    private void addZipEntry(ZipOutputStream zipOutputStream, String name, String content) throws java.io.IOException {
        zipOutputStream.putNextEntry(new ZipEntry(name));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }
}
