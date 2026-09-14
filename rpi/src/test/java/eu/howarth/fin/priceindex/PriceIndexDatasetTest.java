package eu.howarth.fin.priceindex;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriceIndexDatasetTest {

    // 2024: Jan=100, Feb=110, Mar=120 — average=110
    private static final PriceIndexDataset DATASET = new PriceIndexDataset(IndexSeries.CPI, List.of(
            new PriceIndexEntry(2024, 1,  bd("100")),
            new PriceIndexEntry(2024, 2,  bd("110")),
            new PriceIndexEntry(2024, 3,  bd("120")),
            new PriceIndexEntry(2025, 6,  bd("200"))
    ));

    @Test
    void indexForMonth_returnsExactEntry() {
        assertAmount("110", DATASET.indexForMonth(2024, 2));
    }

    @Test
    void indexForYear_averagesAllMonthsInYear() {
        // (100 + 110 + 120) / 3 = 110
        assertAmount("110", DATASET.indexForYear(2024));
    }

    @Test
    void indexForYear_singleMonth_returnsThatMonth() {
        assertAmount("200", DATASET.indexForYear(2025));
    }

    @Test
    void indexForMonth_unknownMonth_throws() {
        assertThrows(IllegalArgumentException.class, () -> DATASET.indexForMonth(2024, 12));
    }

    @Test
    void indexForYear_unknownYear_throws() {
        assertThrows(IllegalArgumentException.class, () -> DATASET.indexForYear(1999));
    }

    @Test
    void immutability_originalListMutationHasNoEffect() {
        var mutable = new java.util.ArrayList<PriceIndexEntry>();
        mutable.add(new PriceIndexEntry(2000, 1, bd("50")));
        PriceIndexDataset ds = new PriceIndexDataset(IndexSeries.RPI, mutable);
        mutable.clear();
        assertDoesNotThrow(() -> ds.indexForMonth(2000, 1));
    }

    @Test
    void series_isCarriedThrough() {
        assertEquals(IndexSeries.CPI, DATASET.series());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Expected " + expected + " but was " + actual);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
