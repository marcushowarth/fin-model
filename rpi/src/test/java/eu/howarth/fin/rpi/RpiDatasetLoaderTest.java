package eu.howarth.fin.rpi;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class RpiDatasetLoaderTest {

    // Minimal ONS CHAW CSV snippet — metadata, annual, quarterly, and monthly rows
    private static final String ONS_SNIPPET = """
            "Title","RPI All Items Index: Jan 1987=100"
            "CDID","CHAW"
            "Source dataset ID","MM23"
            "PreUnit",""
            "Unit","Index, base year = 100"
            "Release date","22-04-2026"
            "Next release","20 May 2026"
            "Important notes",
            "1987","101.9"
            "1988","106.9"
            "2026 Q1","408.7"
            "1987 JAN","100.0"
            "1987 FEB","100.4"
            "1987 MAR","100.6"
            "1988 JAN","103.3"
            "1988 DEC","110.3"
            """;

    @Test
    void fromCsv_parsesMonthlyRows() {
        RpiDataset ds = load(ONS_SNIPPET);
        assertEquals(5, ds.entries().size());
    }

    @Test
    void fromCsv_skipsMetadataAnnualAndQuarterlyRows() {
        RpiDataset ds = load(ONS_SNIPPET);
        // Only monthly rows: 1987 JAN/FEB/MAR, 1988 JAN/DEC
        assertDoesNotThrow(() -> ds.indexForMonth(1987, 1));
        assertThrows(IllegalArgumentException.class, () -> ds.indexForMonth(1987, 4));
    }

    @Test
    void fromCsv_parsesYearMonthAndIndex() {
        RpiDataset ds = load(ONS_SNIPPET);
        assertEquals(0, new BigDecimal("100.0").compareTo(ds.indexForMonth(1987, 1)));
        assertEquals(0, new BigDecimal("100.4").compareTo(ds.indexForMonth(1987, 2)));
        assertEquals(0, new BigDecimal("110.3").compareTo(ds.indexForMonth(1988, 12)));
    }

    @Test
    void fromCsv_allTwelveMonthAbbreviations() {
        String csv = """
                "1990 JAN","1"
                "1990 FEB","2"
                "1990 MAR","3"
                "1990 APR","4"
                "1990 MAY","5"
                "1990 JUN","6"
                "1990 JUL","7"
                "1990 AUG","8"
                "1990 SEP","9"
                "1990 OCT","10"
                "1990 NOV","11"
                "1990 DEC","12"
                """;
        RpiDataset ds = load(csv);
        assertEquals(12, ds.entries().size());
        for (int m = 1; m <= 12; m++) {
            int month = m;
            assertDoesNotThrow(() -> ds.indexForMonth(1990, month));
        }
    }

    @Test
    void bundled_loadsSuccessfully() {
        RpiDataset ds = RpiDatasetLoader.bundled();
        assertFalse(ds.entries().isEmpty());
    }

    @Test
    void bundled_hasKnownBaseMonth() {
        // ONS CHAW base: Jan 1987 = 100.0
        RpiDataset ds = RpiDatasetLoader.bundled();
        assertEquals(0, new BigDecimal("100.0").compareTo(ds.indexForMonth(1987, 1)));
    }

    @Test
    void bundled_hasRecentData() {
        RpiDataset ds = RpiDatasetLoader.bundled();
        assertDoesNotThrow(() -> ds.indexForMonth(2024, 1));
    }

    private static RpiDataset load(String csv) {
        InputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        try {
            return RpiDatasetLoader.fromCsv(in);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
