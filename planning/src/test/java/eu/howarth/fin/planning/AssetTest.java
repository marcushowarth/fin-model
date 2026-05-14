package eu.howarth.fin.planning;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.NavigableMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AssetTest {

    private static final YearMonth JAN_2024 = YearMonth.of(2024, 1);
    private static final YearMonth JUN_2026 = YearMonth.of(2026, 6);

    // Zero growth — clean values for structural tests
    private static final Asset STATIC = new Asset(
            "flat", "family home", JAN_2024, bd("200000"), BigDecimal.ZERO, Optional.empty());

    private static final Asset WITH_SALE = new Asset(
            "flat", "family home", JAN_2024, bd("200000"), BigDecimal.ZERO, Optional.of(JUN_2026));

    // Appreciating asset — 3% annual
    private static final Asset APPRECIATING = new Asset(
            "investment property", "buy to let", JAN_2024, bd("200000"), bd("0.03"), Optional.empty());

    // Depreciating asset — -15% annual (car)
    private static final Asset DEPRECIATING = new Asset(
            "car", "family car", JAN_2024, bd("30000"), bd("-0.15"), Optional.empty());

    // --- Positions: structural ---

    @Test
    void positions_atStartMonth_returnsStartValue() {
        var pos = STATIC.positions(JAN_2024, JAN_2024);
        assertAmount("200000", pos.get(JAN_2024));
    }

    @Test
    void positions_beforeStart_returnsEmpty() {
        var pos = STATIC.positions(YearMonth.of(2020, 1), YearMonth.of(2023, 12));
        assertTrue(pos.isEmpty());
    }

    @Test
    void positions_rangeAfterStart_containsOneEntryPerMonth() {
        var pos = STATIC.positions(JAN_2024, YearMonth.of(2024, 3));
        assertEquals(3, pos.size());
    }

    @Test
    void positions_zeroRate_allMonthsSameValue() {
        var pos = STATIC.positions(JAN_2024, YearMonth.of(2024, 6));
        pos.values().forEach(v -> assertAmount("200000", v));
    }

    // --- Positions: growth ---

    @Test
    void positions_positiveRate_valueIncreasesEachMonth() {
        var pos = APPRECIATING.positions(JAN_2024, YearMonth.of(2025, 1));
        var months = pos.sequencedValues().stream().toList();
        for (int i = 1; i < months.size(); i++) {
            assertTrue(months.get(i).compareTo(months.get(i - 1)) > 0, "Value should increase each month");
        }
    }

    @Test
    void positions_positiveRate_afterOneYear_approximatesAnnualRate() {
        // £200,000 at 3% annual → ~£206,000 after 12 months
        var pos = APPRECIATING.positions(JAN_2024, YearMonth.of(2025, 1));
        BigDecimal yearEnd = pos.get(YearMonth.of(2025, 1));
        assertTrue(yearEnd.compareTo(bd("205800")) > 0 && yearEnd.compareTo(bd("206200")) < 0,
                () -> "Expected ~206000, got " + yearEnd);
    }

    @Test
    void positions_negativeRate_valueDecreasesEachMonth() {
        var pos = DEPRECIATING.positions(JAN_2024, YearMonth.of(2025, 1));
        var months = pos.sequencedValues().stream().toList();
        for (int i = 1; i < months.size(); i++) {
            assertTrue(months.get(i).compareTo(months.get(i - 1)) < 0, "Value should decrease each month");
        }
    }

    @Test
    void positions_negativeRate_neverBelowZero() {
        // Extreme depreciation — value must floor at zero
        var extreme = new Asset("old_car", "write-off", JAN_2024, bd("1000"), bd("-0.99"), Optional.empty());
        var pos = extreme.positions(JAN_2024, YearMonth.of(2040, 1));
        pos.values().forEach(v ->
                assertTrue(v.compareTo(BigDecimal.ZERO) >= 0, "Position must not go negative"));
    }

    // --- Positions: sale date ---

    @Test
    void positions_withSaleDate_stopsMonthBeforeSale() {
        var pos = WITH_SALE.positions(JAN_2024, YearMonth.of(2030, 1));
        assertFalse(pos.containsKey(JUN_2026), "No position on sale date — asset converted to cash");
        assertTrue(pos.containsKey(JUN_2026.minusMonths(1)), "Position present up to month before sale");
    }

    @Test
    void positions_rangeFullyAfterSaleDate_returnsEmpty() {
        var pos = WITH_SALE.positions(YearMonth.of(2027, 1), YearMonth.of(2028, 1));
        assertTrue(pos.isEmpty());
    }

    // --- Flows ---

    @Test
    void flows_noSaleDate_returnsEmpty() {
        var flows = STATIC.flows(JAN_2024, YearMonth.of(2030, 1));
        assertTrue(flows.isEmpty());
    }

    @Test
    void flows_saleDate_inRange_returnsSingleEntryAtSaleDate() {
        var flows = WITH_SALE.flows(JAN_2024, YearMonth.of(2030, 1));
        assertEquals(1, flows.size());
        assertTrue(flows.containsKey(JUN_2026));
    }

    @Test
    void flows_saleDate_proceedsEqualStartValueForZeroGrowth() {
        // Zero growth → value at sale date = startValue
        NavigableMap<YearMonth, BigDecimal> flows = WITH_SALE.flows(JAN_2024, YearMonth.of(2030, 1));
        assertAmount("200000", flows.get(JUN_2026));
    }

    @Test
    void flows_saleProceeds_arePositive() {
        var flows = WITH_SALE.flows(JAN_2024, YearMonth.of(2030, 1));
        assertTrue(flows.get(JUN_2026).compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void flows_saleDate_outsideQueryRange_returnsEmpty() {
        var flows = WITH_SALE.flows(YearMonth.of(2020, 1), YearMonth.of(2025, 1));
        assertTrue(flows.isEmpty());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Expected " + expected + " but was " + actual);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
