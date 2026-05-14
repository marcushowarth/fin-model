package eu.howarth.fin.rpi.projection;

import eu.howarth.fin.rpi.RpiAdjuster;
import eu.howarth.fin.rpi.RpiDataset;
import eu.howarth.fin.rpi.RpiEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RpiProjectorTest {

    private static final RpiDataset HISTORICAL = new RpiDataset(List.of(
            new RpiEntry(2023, 11, bd("295")),
            new RpiEntry(2023, 12, bd("296")),
            new RpiEntry(2024,  1, bd("100"))
    ));

    @Test
    void historicalEntriesArePreserved() {
        var combined = RpiProjector.project(HISTORICAL, new ConstantInflationProjection(BigDecimal.ZERO), YearMonth.of(2024, 3));
        assertAmount("295", combined.indexForMonth(2023, 11));
        assertAmount("296", combined.indexForMonth(2023, 12));
        assertAmount("100", combined.indexForMonth(2024,  1));
    }

    @Test
    void projectedEntriesAreAccessible() {
        var combined = RpiProjector.project(HISTORICAL, new ConstantInflationProjection(BigDecimal.ZERO), YearMonth.of(2024, 3));
        assertAmount("100", combined.indexForMonth(2024, 2));
        assertAmount("100", combined.indexForMonth(2024, 3));
    }

    @Test
    void anchorIsLastHistoricalEntry() {
        var combined = RpiProjector.project(HISTORICAL, new ConstantInflationProjection(BigDecimal.ZERO), YearMonth.of(2024, 2));
        assertEquals(1, combined.entries().stream()
                .filter(e -> e.year() == 2024 && e.month() == 1).count(),
                "Anchor should not be duplicated in combined dataset");
    }

    @Test
    void upToAtAnchor_returnsJustHistorical() {
        var combined = RpiProjector.project(HISTORICAL, new ConstantInflationProjection(BigDecimal.ZERO), YearMonth.of(2024, 1));
        assertThrows(IllegalArgumentException.class, () -> combined.indexForMonth(2024, 2));
    }

    @Test
    void positiveRate_projectedIndexExceedsAnchor() {
        var combined = RpiProjector.project(HISTORICAL, new ConstantInflationProjection(bd("0.03")), YearMonth.of(2024, 2));
        assertTrue(combined.indexForMonth(2024, 2).compareTo(bd("100")) > 0);
    }

    @Test
    void rpiAdjuster_worksAcrossHistoricalAndProjectedBoundary() {
        var combined = RpiProjector.project(HISTORICAL, new ConstantInflationProjection(BigDecimal.ZERO), YearMonth.of(2024, 3));
        BigDecimal adjusted = RpiAdjuster.adjust(bd("250"), YearMonth.of(2024, 1), YearMonth.of(2024, 2), combined);
        assertAmount("250", adjusted);
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Expected " + expected + " but was " + actual);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
