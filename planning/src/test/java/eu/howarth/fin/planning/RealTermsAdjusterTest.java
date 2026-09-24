package eu.howarth.fin.planning;

import eu.howarth.fin.priceindex.IndexSeries;
import eu.howarth.fin.priceindex.PriceIndexDataset;
import eu.howarth.fin.priceindex.PriceIndexEntry;
import eu.howarth.fin.priceindex.scenario.IndexScenario;
import eu.howarth.fin.priceindex.scenario.IndexScenarioSet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class RealTermsAdjusterTest {

    private static final YearMonth BASE = YearMonth.of(2026, 1);
    private static final YearMonth MONTH_2 = YearMonth.of(2026, 2);
    private static final YearMonth MONTH_3 = YearMonth.of(2026, 3);

    // low: 2% per month — 2026-03 index 104, so deflating 1040 → 1040*(100/104) = 1000 exactly
    private static final PriceIndexDataset LOW_DATASET = new PriceIndexDataset(IndexSeries.RPI, List.of(
            new PriceIndexEntry(2026, 1, bd("100")),
            new PriceIndexEntry(2026, 2, bd("102")),
            new PriceIndexEntry(2026, 3, bd("104"))
    ));

    // high: 5% per month — 2026-03 index 110
    private static final PriceIndexDataset HIGH_DATASET = new PriceIndexDataset(IndexSeries.RPI, List.of(
            new PriceIndexEntry(2026, 1, bd("100")),
            new PriceIndexEntry(2026, 2, bd("105")),
            new PriceIndexEntry(2026, 3, bd("110"))
    ));

    private static final IndexScenarioSet SCENARIOS = new IndexScenarioSet(List.of(
            new IndexScenario("low", LOW_DATASET),
            new IndexScenario("high", HIGH_DATASET)
    ));

    // Nominal net worth: 1000, 1020, 1040 over three months
    private static final ModelProjection NOMINAL;

    static {
        NavigableMap<YearMonth, BigDecimal> netWorth = new TreeMap<>();
        netWorth.put(BASE,    bd("1000"));
        netWorth.put(MONTH_2, bd("1020"));
        netWorth.put(MONTH_3, bd("1040"));

        NavigableMap<YearMonth, BigDecimal> cashPosition = new TreeMap<>();
        cashPosition.put(BASE,    bd("500"));
        cashPosition.put(MONTH_2, bd("510"));
        cashPosition.put(MONTH_3, bd("520"));

        NavigableMap<YearMonth, BigDecimal> itemSeries = new TreeMap<>();
        itemSeries.put(BASE,    bd("600"));
        itemSeries.put(MONTH_3, bd("624"));

        NOMINAL = new ModelProjection(netWorth, cashPosition, Map.of("House", itemSeries), Map.of(), List.of());
    }

    @Test
    void scenarioNames_presentInNetWorth() {
        RealTermsProjection result = RealTermsAdjuster.adjust(NOMINAL, SCENARIOS, BASE);
        assertTrue(result.netWorth().containsKey("low"));
        assertTrue(result.netWorth().containsKey("high"));
    }

    @Test
    void baseMonth_netWorthIsUnchangedInAllScenarios() {
        RealTermsProjection result = RealTermsAdjuster.adjust(NOMINAL, SCENARIOS, BASE);
        assertAmount("1000", result.netWorth().get("low").get(BASE));
        assertAmount("1000", result.netWorth().get("high").get(BASE));
    }

    @Test
    void futureMonth_lowInflation_givesHigherRealNetWorth() {
        RealTermsProjection result = RealTermsAdjuster.adjust(NOMINAL, SCENARIOS, BASE);
        BigDecimal lowReal  = result.netWorth().get("low").get(MONTH_3);
        BigDecimal highReal = result.netWorth().get("high").get(MONTH_3);
        assertTrue(lowReal.compareTo(highReal) > 0,
                "Lower inflation should give higher real value; low=" + lowReal + " high=" + highReal);
    }

    @Test
    void futureMonth_lowScenario_deflatesExactly() {
        // nominal=1040, RPI 104→100: 1040*(100/104) = 1000 exactly
        RealTermsProjection result = RealTermsAdjuster.adjust(NOMINAL, SCENARIOS, BASE);
        assertAmount("1000", result.netWorth().get("low").get(MONTH_3));
    }

    @Test
    void cashPosition_deflatedByScenario() {
        RealTermsProjection result = RealTermsAdjuster.adjust(NOMINAL, SCENARIOS, BASE);
        assertAmount("500", result.cashPosition().get("low").get(BASE));
        assertAmount("500", result.cashPosition().get("high").get(BASE));
        // low: 520*(100/104) = 500 exactly
        assertAmount("500", result.cashPosition().get("low").get(MONTH_3));
    }

    @Test
    void cashPosition_negativeValue_deflatesBySameRatioAsPositive() {
        // A cash shortfall (overdraft) deflates by the exact same RPI ratio as a surplus —
        // inflation erodes the real burden of a fixed nominal liability just as it erodes
        // the real value of a fixed nominal asset. Mirrors cashPosition_deflatedByScenario
        // above with the sign flipped: -520*(100/104) = -500 exactly, shrinking toward zero
        // rather than growing more negative.
        NavigableMap<YearMonth, BigDecimal> netWorth = new TreeMap<>();
        netWorth.put(BASE,    bd("-500"));
        netWorth.put(MONTH_3, bd("-520"));

        NavigableMap<YearMonth, BigDecimal> cashPosition = new TreeMap<>();
        cashPosition.put(BASE,    bd("-500"));
        cashPosition.put(MONTH_3, bd("-520"));

        ModelProjection negative = new ModelProjection(netWorth, cashPosition, Map.of(), Map.of(), List.of());
        RealTermsProjection result = RealTermsAdjuster.adjust(negative, SCENARIOS, BASE);

        assertAmount("-500", result.cashPosition().get("low").get(BASE));
        assertAmount("-500", result.cashPosition().get("low").get(MONTH_3));
    }

    @Test
    void itemPositions_deflatedByScenario() {
        RealTermsProjection result = RealTermsAdjuster.adjust(NOMINAL, SCENARIOS, BASE);
        // House at base is unchanged
        assertAmount("600", result.itemPositions().get("low").get("House").get(BASE));
        assertAmount("600", result.itemPositions().get("high").get("House").get(BASE));
        // low: 624*(100/104) = 600 exactly
        assertAmount("600", result.itemPositions().get("low").get("House").get(MONTH_3));
        // high inflation erodes real value more
        BigDecimal lowHouse  = result.itemPositions().get("low").get("House").get(MONTH_3);
        BigDecimal highHouse = result.itemPositions().get("high").get("House").get(MONTH_3);
        assertTrue(lowHouse.compareTo(highHouse) > 0);
    }

    @Test
    void baseMonth_isRecorded() {
        RealTermsProjection result = RealTermsAdjuster.adjust(NOMINAL, SCENARIOS, BASE);
        assertEquals(BASE, result.base());
    }

    @Test
    void allMonths_presentInEachScenario() {
        RealTermsProjection result = RealTermsAdjuster.adjust(NOMINAL, SCENARIOS, BASE);
        assertEquals(3, result.netWorth().get("low").size());
        assertEquals(3, result.netWorth().get("high").size());
    }

    @Test
    void emptyItemPositions_producesEmptyScenarioItemMap() {
        NavigableMap<YearMonth, BigDecimal> netWorth = new TreeMap<>();
        netWorth.put(BASE, bd("1000"));
        ModelProjection noItems = new ModelProjection(netWorth, new TreeMap<>(), Map.of(), Map.of(), List.of());
        RealTermsProjection result = RealTermsAdjuster.adjust(noItems, SCENARIOS, BASE);
        assertTrue(result.itemPositions().isEmpty());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertNotNull(actual, "Expected " + expected + " but value was null");
        assertEquals(0,
                new BigDecimal(expected).setScale(2, RoundingMode.HALF_UP)
                        .compareTo(actual.setScale(2, RoundingMode.HALF_UP)),
                () -> "Expected " + expected + " but was " + actual);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
