package es.checkpol.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InePostalMappingsZipParserTest {

    private final InePostalMappingsZipParser parser = new InePostalMappingsZipParser();

    @Test
    void convertsOfficialTramZipIntoInternalCsv() throws Exception {
        byte[] zipBytes = zipWithEntry(
            "caj_esp_072025/TRAM.D250630.G250702",
            tramLine("28079", "28001") + "\n" + tramLine("35014", "35540") + "\n" + tramLine("28079", "00000") + "\n"
        );

        Optional<byte[]> converted = parser.tryConvertToInternalCsv(zipBytes);

        assertTrue(converted.isPresent());
        assertEquals("""
            postalCode;municipalityCode
            28001;28079
            35540;35014
            """, new String(converted.get(), StandardCharsets.UTF_8));
    }

    @Test
    void returnsEmptyWhenZipDoesNotContainOfficialTramFile() throws Exception {
        byte[] zipBytes = zipWithEntry("postal.csv", "postalCode;municipalityCode\n28001;28079\n");

        Optional<byte[]> converted = parser.tryConvertToInternalCsv(zipBytes);

        assertTrue(converted.isEmpty());
    }

    private byte[] zipWithEntry(String name, String content) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.ISO_8859_1)) {
            zipOutputStream.putNextEntry(new ZipEntry(name));
            zipOutputStream.write(content.getBytes(StandardCharsets.ISO_8859_1));
            zipOutputStream.closeEntry();
        }
        return outputStream.toByteArray();
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
}
