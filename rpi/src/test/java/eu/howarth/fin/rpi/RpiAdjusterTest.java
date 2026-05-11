package eu.howarth.fin.rpi;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RpiAdjusterTest {

    // Powers of 2 for easy mental arithmetic: 2000=100, 2010=200, 2020=400
    private static final RpiDataset TEST_DATA = new RpiDataset(List.of(
            new RpiEntry(2000, bd("100")),
            new RpiEntry(2010, bd("200")),
            new RpiEntry(2020, bd("400"))
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

    // BigDecimal equality ignoring scale (200.0 == 200.00)
    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Expected " + expected + " but was " + actual);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
