package eu.howarth.fin.rpi;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RpiAdjusterTest {

    // 3 months per year averaging to 100/200/400 — representative of real ONS annual = avg(monthly)
    private static final RpiDataset TEST_DATA = new RpiDataset(List.of(
            new RpiEntry(2000,  1, bd("80")),  new RpiEntry(2000,  6, bd("100")),  new RpiEntry(2000, 12, bd("120")),
            new RpiEntry(2010,  1, bd("160")), new RpiEntry(2010,  6, bd("200")),  new RpiEntry(2010, 12, bd("240")),
            new RpiEntry(2020,  1, bd("320")), new RpiEntry(2020,  6, bd("400")),  new RpiEntry(2020, 12, bd("480"))
    ));

    // Monthly test data: Jan-2000=100, Jun-2010=200, Dec-2020=400 (powers of 2 — divide evenly, no rounding error)
    private static final RpiDataset MONTHLY_DATA = new RpiDataset(List.of(
            new RpiEntry(2000,  1, bd("100")),
            new RpiEntry(2010,  6, bd("200")),
            new RpiEntry(2020, 12, bd("400"))
    ));

    @Test
    void sameYear_returnsOriginalValue() {
        assertAmount("500", RpiAdjuster.adjust(bd("500"), 2010, 2010, TEST_DATA));
    }

    @Test
    void forwardInTime_scalesUpByRpiRatio() {
        // £100 in 2000 (RPI=100) → 2010 (RPI=200) = £200
        assertAmount("200", RpiAdjuster.adjust(bd("100"), 2000, 2010, TEST_DATA));
    }

    @Test
    void backwardInTime_scalesDownByRpiRatio() {
        // £200 in 2010 (RPI=200) → 2000 (RPI=100) = £100
        assertAmount("100", RpiAdjuster.adjust(bd("200"), 2010, 2000, TEST_DATA));
    }

    @Test
    void negativeValue_backwardInTime_shrinksMagnitudeBySameRatio() {
        // -£200 in 2010 (RPI=200) deflated to 2000 terms (RPI=100) = -£100.
        // Same ratio as the positive case above — inflation erodes the real burden of a
        // fixed nominal liability just as it erodes the real value of a fixed nominal asset.
        assertAmount("-100", RpiAdjuster.adjust(bd("-200"), 2010, 2000, TEST_DATA));
    }

    @Test
    void nonBaseYear_usesCorrectRatio() {
        // £100 in 2010 (RPI=200) → 2020 (RPI=400) = £200
        assertAmount("200", RpiAdjuster.adjust(bd("100"), 2010, 2020, TEST_DATA));
    }

    @Test
    void zeroValue_returnsZero() {
        assertAmount("0", RpiAdjuster.adjust(BigDecimal.ZERO, 2000, 2020, TEST_DATA));
    }

    @Test
    void roundTrip_returnsOriginalValue() {
        BigDecimal original = bd("250");
        BigDecimal there = RpiAdjuster.adjust(original, 2000, 2020, TEST_DATA);
        BigDecimal back  = RpiAdjuster.adjust(there,    2020, 2000, TEST_DATA);
        assertEquals(0, original.compareTo(back), () -> "Round trip failed: got " + back);
    }

    @Test
    void unknownYear_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                RpiAdjuster.adjust(bd("100"), 2000, 2015, TEST_DATA));
    }

    // --- monthly overload ---

    @Test
    void monthly_sameYearMonth_returnsOriginalValue() {
        assertAmount("500", RpiAdjuster.adjust(bd("500"), YearMonth.of(2010, 6), YearMonth.of(2010, 6), MONTHLY_DATA));
    }

    @Test
    void monthly_forwardInTime_scalesUpByRpiRatio() {
        // £100 in 2000-01 (RPI=100) → 2010-06 (RPI=200) = £200
        assertAmount("200", RpiAdjuster.adjust(bd("100"), YearMonth.of(2000, 1), YearMonth.of(2010, 6), MONTHLY_DATA));
    }

    @Test
    void monthly_backwardInTime_scalesDownByRpiRatio() {
        // £200 in 2010-06 (RPI=200) → 2000-01 (RPI=100) = £100
        assertAmount("100", RpiAdjuster.adjust(bd("200"), YearMonth.of(2010, 6), YearMonth.of(2000, 1), MONTHLY_DATA));
    }

    @Test
    void monthly_roundTrip_returnsOriginalValue() {
        BigDecimal original = bd("250");
        BigDecimal there = RpiAdjuster.adjust(original, YearMonth.of(2000, 1), YearMonth.of(2020, 12), MONTHLY_DATA);
        BigDecimal back  = RpiAdjuster.adjust(there,    YearMonth.of(2020, 12), YearMonth.of(2000, 1), MONTHLY_DATA);
        assertEquals(0, original.compareTo(back), () -> "Round trip failed: got " + back);
    }

    @Test
    void monthly_unknownMonth_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                RpiAdjuster.adjust(bd("100"), YearMonth.of(2000, 1), YearMonth.of(2010, 3), MONTHLY_DATA));
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
