package eu.howarth.fin.priceindex.scenario;

import eu.howarth.fin.priceindex.IndexSeries;
import eu.howarth.fin.priceindex.PriceIndexDataset;
import eu.howarth.fin.priceindex.PriceIndexEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndexScenarioSetTest {

    private static final PriceIndexDataset LOW_DATASET = new PriceIndexDataset(IndexSeries.RPI, List.of(
            new PriceIndexEntry(2024, 1, bd("100")),
            new PriceIndexEntry(2025, 1, bd("103"))
    ));

    private static final PriceIndexDataset HIGH_DATASET = new PriceIndexDataset(IndexSeries.RPI, List.of(
            new PriceIndexEntry(2024, 1, bd("100")),
            new PriceIndexEntry(2025, 1, bd("106"))
    ));

    private static final IndexScenarioSet SCENARIOS = new IndexScenarioSet(List.of(
            new IndexScenario("low",  LOW_DATASET),
            new IndexScenario("high", HIGH_DATASET)
    ));

    @Test
    void scenarioNames_arePreserved() {
        Map<String, BigDecimal> result = SCENARIOS.adjust(bd("100"), YearMonth.of(2024, 1), YearMonth.of(2025, 1));
        assertTrue(result.containsKey("low"));
        assertTrue(result.containsKey("high"));
    }

    @Test
    void adjust_returnsOneEntryPerScenario() {
        Map<String, BigDecimal> result = SCENARIOS.adjust(bd("100"), YearMonth.of(2024, 1), YearMonth.of(2025, 1));
        assertEquals(2, result.size());
    }

    @Test
    void adjust_higherInflation_producesHigherNominalValue() {
        Map<String, BigDecimal> result = SCENARIOS.adjust(bd("100"), YearMonth.of(2024, 1), YearMonth.of(2025, 1));
        assertTrue(result.get("high").compareTo(result.get("low")) > 0,
                "Higher inflation should produce a higher nominal future value");
    }

    @Test
    void adjust_deflatingFutureNominal_higherInflationReducesRealValue() {
        Map<String, BigDecimal> result = SCENARIOS.adjust(bd("100"), YearMonth.of(2025, 1), YearMonth.of(2024, 1));
        assertTrue(result.get("low").compareTo(result.get("high")) > 0,
                "Higher inflation erodes real value more — low scenario should give higher real terms result");
    }

    @Test
    void adjust_sameMonth_returnsOriginalValueInAllScenarios() {
        Map<String, BigDecimal> result = SCENARIOS.adjust(bd("500"), YearMonth.of(2024, 1), YearMonth.of(2024, 1));
        assertAmount("500", result.get("low"));
        assertAmount("500", result.get("high"));
    }

    @Test
    void immutability_originalListMutationHasNoEffect() {
        var mutable = new java.util.ArrayList<>(List.of(new IndexScenario("base", LOW_DATASET)));
        var set = new IndexScenarioSet(mutable);
        mutable.clear();
        assertEquals(1, set.scenarios().size());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Expected " + expected + " but was " + actual);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
