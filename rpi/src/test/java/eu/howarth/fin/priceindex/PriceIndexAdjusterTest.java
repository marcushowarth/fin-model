package eu.howarth.fin.priceindex;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriceIndexAdjusterTest {

    // 3 months per year averaging to 100/200/400 — representative of real ONS annual = avg(monthly)
    private static final PriceIndexDataset TEST_DATA = new PriceIndexDataset(IndexSeries.CPI, List.of(
            new PriceIndexEntry(2000,  1, bd("80")),  new PriceIndexEntry(2000,  6, bd("100")),  new PriceIndexEntry(2000, 12, bd("120")),
            new PriceIndexEntry(2010,  1, bd("160")), new PriceIndexEntry(2010,  6, bd("200")),  new PriceIndexEntry(2010, 12, bd("240")),
            new PriceIndexEntry(2020,  1, bd("320")), new PriceIndexEntry(2020,  6, bd("400")),  new PriceIndexEntry(2020, 12, bd("480"))
    ));

    // Monthly test data: Jan-2000=100, Jun-2010=200, Dec-2020=400 (powers of 2 — divide evenly, no rounding error)
    private static final PriceIndexDataset MONTHLY_DATA = new PriceIndexDataset(IndexSeries.CPI, List.of(
            new PriceIndexEntry(2000,  1, bd("100")),
            new PriceIndexEntry(2010,  6, bd("200")),
            new PriceIndexEntry(2020, 12, bd("400"))
    ));

    @Test
    void sameYear_returnsOriginalValue() {
        assertAmount("500", PriceIndexAdjuster.adjust(bd("500"), 2010, 2010, TEST_DATA));
    }

    @Test
    void forwardInTime_scalesUpByIndexRatio() {
        // £100 in 2000 (index=100) → 2010 (index=200) = £200
        assertAmount("200", PriceIndexAdjuster.adjust(bd("100"), 2000, 2010, TEST_DATA));
    }

    @Test
    void backwardInTime_scalesDownByIndexRatio() {
        // £200 in 2010 (index=200) → 2000 (index=100) = £100
        assertAmount("100", PriceIndexAdjuster.adjust(bd("200"), 2010, 2000, TEST_DATA));
    }

    @Test
    void negativeValue_backwardInTime_shrinksMagnitudeBySameRatio() {
        assertAmount("-100", PriceIndexAdjuster.adjust(bd("-200"), 2010, 2000, TEST_DATA));
    }

    @Test
    void nonBaseYear_usesCorrectRatio() {
        // £100 in 2010 (index=200) → 2020 (index=400) = £200
        assertAmount("200", PriceIndexAdjuster.adjust(bd("100"), 2010, 2020, TEST_DATA));
    }

    @Test
    void zeroValue_returnsZero() {
        assertAmount("0", PriceIndexAdjuster.adjust(BigDecimal.ZERO, 2000, 2020, TEST_DATA));
    }

    @Test
    void roundTrip_returnsOriginalValue() {
        BigDecimal original = bd("250");
        BigDecimal there = PriceIndexAdjuster.adjust(original, 2000, 2020, TEST_DATA);
        BigDecimal back  = PriceIndexAdjuster.adjust(there,    2020, 2000, TEST_DATA);
        assertEquals(0, original.compareTo(back), () -> "Round trip failed: got " + back);
    }

    @Test
    void unknownYear_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                PriceIndexAdjuster.adjust(bd("100"), 2000, 2015, TEST_DATA));
    }

    // --- monthly overload ---

    @Test
    void monthly_sameYearMonth_returnsOriginalValue() {
        assertAmount("500", PriceIndexAdjuster.adjust(bd("500"), YearMonth.of(2010, 6), YearMonth.of(2010, 6), MONTHLY_DATA));
    }

    @Test
    void monthly_forwardInTime_scalesUpByIndexRatio() {
        assertAmount("200", PriceIndexAdjuster.adjust(bd("100"), YearMonth.of(2000, 1), YearMonth.of(2010, 6), MONTHLY_DATA));
    }

    @Test
    void monthly_backwardInTime_scalesDownByIndexRatio() {
        assertAmount("100", PriceIndexAdjuster.adjust(bd("200"), YearMonth.of(2010, 6), YearMonth.of(2000, 1), MONTHLY_DATA));
    }

    @Test
    void monthly_roundTrip_returnsOriginalValue() {
        BigDecimal original = bd("250");
        BigDecimal there = PriceIndexAdjuster.adjust(original, YearMonth.of(2000, 1), YearMonth.of(2020, 12), MONTHLY_DATA);
        BigDecimal back  = PriceIndexAdjuster.adjust(there,    YearMonth.of(2020, 12), YearMonth.of(2000, 1), MONTHLY_DATA);
        assertEquals(0, original.compareTo(back), () -> "Round trip failed: got " + back);
    }

    @Test
    void monthly_unknownMonth_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                PriceIndexAdjuster.adjust(bd("100"), YearMonth.of(2000, 1), YearMonth.of(2010, 3), MONTHLY_DATA));
    }

    // BigDecimal equality ignoring scale (200.0 == 200.00)
    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Expected " + expected + " but was " + actual);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
