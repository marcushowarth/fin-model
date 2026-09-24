package eu.howarth.fin.priceindex.projection;

import eu.howarth.fin.priceindex.PriceIndexEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConstantIndexProjectionTest {

    private static final PriceIndexEntry ANCHOR = new PriceIndexEntry(2024, 1, bd("100"));

    @Test
    void upToEqualsAnchor_returnsEmpty() {
        var projection = new ConstantIndexProjection(BigDecimal.ZERO);
        assertTrue(projection.project(ANCHOR, YearMonth.of(2024, 1)).isEmpty());
    }

    @Test
    void zeroRate_correctMonthCount() {
        var projection = new ConstantIndexProjection(BigDecimal.ZERO);
        List<PriceIndexEntry> entries = projection.project(ANCHOR, YearMonth.of(2024, 3));
        assertEquals(2, entries.size());
    }

    @Test
    void zeroRate_correctMonths() {
        var projection = new ConstantIndexProjection(BigDecimal.ZERO);
        List<PriceIndexEntry> entries = projection.project(ANCHOR, YearMonth.of(2024, 3));
        assertEquals(2024, entries.get(0).year());
        assertEquals(2,    entries.get(0).month());
        assertEquals(2024, entries.get(1).year());
        assertEquals(3,    entries.get(1).month());
    }

    @Test
    void zeroRate_indexRemainsConstant() {
        var projection = new ConstantIndexProjection(BigDecimal.ZERO);
        List<PriceIndexEntry> entries = projection.project(ANCHOR, YearMonth.of(2024, 3));
        entries.forEach(e -> assertAmount("100", e.index()));
    }

    @Test
    void crossesYearBoundary_correctMonthCount() {
        var projection = new ConstantIndexProjection(BigDecimal.ZERO);
        List<PriceIndexEntry> entries = projection.project(ANCHOR, YearMonth.of(2025, 1));
        assertEquals(12, entries.size());
        var last = entries.get(entries.size() - 1);
        assertEquals(2025, last.year());
        assertEquals(1,    last.month());
    }

    @Test
    void positiveRate_firstMonthIndexExceedsAnchor() {
        var projection = new ConstantIndexProjection(bd("0.12"));
        List<PriceIndexEntry> entries = projection.project(ANCHOR, YearMonth.of(2024, 2));
        assertEquals(1, entries.size());
        assertTrue(entries.get(0).index().compareTo(bd("100")) > 0);
    }

    @Test
    void positiveRate_indexGrowsMonotonically() {
        var projection = new ConstantIndexProjection(bd("0.03"));
        List<PriceIndexEntry> entries = projection.project(ANCHOR, YearMonth.of(2025, 1));
        for (int i = 1; i < entries.size(); i++) {
            assertTrue(entries.get(i).index().compareTo(entries.get(i - 1).index()) > 0,
                    "Index should increase each month");
        }
    }

    @Test
    void positiveRate_afterOneYear_approximatesAnnualRate() {
        var projection = new ConstantIndexProjection(bd("0.03"));
        List<PriceIndexEntry> entries = projection.project(ANCHOR, YearMonth.of(2025, 1));
        BigDecimal yearEnd = entries.get(entries.size() - 1).index();
        assertTrue(yearEnd.compareTo(bd("102.9")) > 0 && yearEnd.compareTo(bd("103.1")) < 0,
                () -> "Expected ~103, got " + yearEnd);
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Expected " + expected + " but was " + actual);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
