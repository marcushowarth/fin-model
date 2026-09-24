package eu.howarth.fin.priceindex;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriceIndexDatasetLoaderTest {

    // Minimal ONS CSV snippet — metadata, annual, quarterly, and monthly rows.
    // Same export shape regardless of series (CHAW/RPI or D7BT/CPI).
    private static final String ONS_SNIPPET = """
            "Title","CPI INDEX 00: ALL ITEMS 2015=100"
            "CDID","D7BT"
            "Source dataset ID","MM23"
            "PreUnit",""
            "Unit","Index, base year = 100"
            "Release date","22-04-2026"
            "Next release","20 May 2026"
            "Important notes",
            "2015","100.0"
            "2016","101.0"
            "2026 Q1","141.0"
            "2015 JAN","99.3"
            "2015 FEB","99.5"
            "2015 MAR","99.7"
            "2016 JAN","100.6"
            "2016 DEC","101.9"
            """;

    @Test
    void fromCsv_parsesMonthlyRows() {
        PriceIndexDataset ds = load(ONS_SNIPPET);
        assertEquals(5, ds.entries().size());
    }

    @Test
    void fromCsv_skipsMetadataAnnualAndQuarterlyRows() {
        PriceIndexDataset ds = load(ONS_SNIPPET);
        // Only monthly rows: 2015 JAN/FEB/MAR, 2016 JAN/DEC
        assertDoesNotThrow(() -> ds.indexForMonth(2015, 1));
        assertThrows(IllegalArgumentException.class, () -> ds.indexForMonth(2015, 4));
    }

    @Test
    void fromCsv_parsesYearMonthAndIndex() {
        PriceIndexDataset ds = load(ONS_SNIPPET);
        assertEquals(0, new BigDecimal("99.3").compareTo(ds.indexForMonth(2015, 1)));
        assertEquals(0, new BigDecimal("99.5").compareTo(ds.indexForMonth(2015, 2)));
        assertEquals(0, new BigDecimal("101.9").compareTo(ds.indexForMonth(2016, 12)));
    }

    @Test
    void fromCsv_taggedWithGivenSeries() {
        PriceIndexDataset ds = load(ONS_SNIPPET);
        assertEquals(IndexSeries.CPI, ds.series());
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
        PriceIndexDataset ds = load(csv);
        assertEquals(12, ds.entries().size());
        for (int m = 1; m <= 12; m++) {
            int month = m;
            assertDoesNotThrow(() -> ds.indexForMonth(1990, month));
        }
    }

    @Test
    void bundled_rpi_loadsSuccessfully() {
        PriceIndexDataset ds = PriceIndexDatasetLoader.bundled(IndexSeries.RPI);
        assertFalse(ds.entries().isEmpty());
        assertEquals(IndexSeries.RPI, ds.series());
    }

    @Test
    void bundled_rpi_hasKnownBaseMonth() {
        // ONS CHAW base: Jan 1987 = 100.0
        PriceIndexDataset ds = PriceIndexDatasetLoader.bundled(IndexSeries.RPI);
        assertEquals(0, new BigDecimal("100.0").compareTo(ds.indexForMonth(1987, 1)));
    }

    @Test
    void bundled_cpi_loadsSuccessfully() {
        PriceIndexDataset ds = PriceIndexDatasetLoader.bundled(IndexSeries.CPI);
        assertFalse(ds.entries().isEmpty());
        assertEquals(IndexSeries.CPI, ds.series());
    }

    @Test
    void bundled_cpi_hasKnownBaseYear() {
        // ONS D7BT base: 2015 = 100 (CPI is base-year-averaged, not a single
        // base month exactly 100.0 the way RPI's Jan 1987 is)
        PriceIndexDataset ds = PriceIndexDatasetLoader.bundled(IndexSeries.CPI);
        BigDecimal base2015 = ds.indexForYear(2015);
        assertTrue(base2015.compareTo(new BigDecimal("99")) > 0 && base2015.compareTo(new BigDecimal("101")) < 0,
                () -> "Expected ~100 for 2015 base year, got " + base2015);
    }

    @Test
    void bundled_cpi_hasRecentData() {
        PriceIndexDataset ds = PriceIndexDatasetLoader.bundled(IndexSeries.CPI);
        assertDoesNotThrow(() -> ds.indexForMonth(2026, 1));
    }

    @Test
    void bundled_rpiAndCpi_divergeOverSamePeriod() {
        // RPI and CPI measure the same economy differently (RPI includes
        // housing costs CPI excludes, among other basket differences) —
        // the two series' year-on-year ratios shouldn't be identical.
        // This is the actual point of offering both (kanban #981).
        PriceIndexDataset rpi = PriceIndexDatasetLoader.bundled(IndexSeries.RPI);
        PriceIndexDataset cpi = PriceIndexDatasetLoader.bundled(IndexSeries.CPI);

        BigDecimal rpiRatio = rpi.indexForYear(2024).divide(rpi.indexForYear(2015), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal cpiRatio = cpi.indexForYear(2024).divide(cpi.indexForYear(2015), 10, java.math.RoundingMode.HALF_UP);

        assertNotEquals(0, rpiRatio.compareTo(cpiRatio),
                () -> "Expected RPI and CPI 2015->2024 ratios to differ, both were " + rpiRatio);
    }

    @Test
    void bundled_rpiMillennium_loadsSuccessfully() {
        PriceIndexDataset ds = PriceIndexDatasetLoader.bundled(IndexSeries.RPI_MILLENNIUM);
        assertFalse(ds.entries().isEmpty());
        assertEquals(IndexSeries.RPI_MILLENNIUM, ds.series());
    }

    @Test
    void bundled_rpiMillennium_spansFrom1209To2016() {
        PriceIndexDataset ds = PriceIndexDatasetLoader.bundled(IndexSeries.RPI_MILLENNIUM);
        assertDoesNotThrow(() -> ds.indexForYear(1209));
        assertDoesNotThrow(() -> ds.indexForYear(2016));
        assertThrows(IllegalArgumentException.class, () -> ds.indexForYear(2017));
    }

    @Test
    void bundled_rpiMillennium_hasKnownBaseYear() {
        PriceIndexDataset ds = PriceIndexDatasetLoader.bundled(IndexSeries.RPI_MILLENNIUM);
        BigDecimal base2015 = ds.indexForYear(2015);
        assertEquals(0, new BigDecimal("100.0").compareTo(base2015));
    }

    @Test
    void bundled_cpiMillennium_loadsSuccessfully() {
        PriceIndexDataset ds = PriceIndexDatasetLoader.bundled(IndexSeries.CPI_MILLENNIUM);
        assertFalse(ds.entries().isEmpty());
        assertEquals(IndexSeries.CPI_MILLENNIUM, ds.series());
    }

    @Test
    void bundled_cpiMillennium_agreesWithLiveCpiOnOverlapYears() {
        // Cross-validated during research (kanban #1003) — Millennium column D and our
        // bundled ONS D7BT should agree within a small tolerance for shared years.
        PriceIndexDataset millennium = PriceIndexDatasetLoader.bundled(IndexSeries.CPI_MILLENNIUM);
        PriceIndexDataset live = PriceIndexDatasetLoader.bundled(IndexSeries.CPI);
        for (int year : List.of(1988, 1990, 2000, 2010, 2014, 2015, 2016)) {
            BigDecimal diff = millennium.indexForYear(year).subtract(live.indexForYear(year)).abs();
            assertTrue(diff.compareTo(new BigDecimal("0.1")) < 0,
                    () -> "Year " + year + ": Millennium=" + millennium.indexForYear(year) + " live=" + live.indexForYear(year));
        }
    }

    private static PriceIndexDataset load(String csv) {
        InputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        try {
            return PriceIndexDatasetLoader.fromCsv(in, IndexSeries.CPI);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
