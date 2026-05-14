package eu.howarth.fin.rpi.scenario;

import eu.howarth.fin.rpi.RpiDataset;
import eu.howarth.fin.rpi.RpiEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RpiScenarioSetTest {

    private static final RpiDataset LOW_DATASET = new RpiDataset(List.of(
            new RpiEntry(2024, 1, bd("100")),
            new RpiEntry(2025, 1, bd("103"))
    ));

    private static final RpiDataset HIGH_DATASET = new RpiDataset(List.of(
            new RpiEntry(2024, 1, bd("100")),
            new RpiEntry(2025, 1, bd("106"))
    ));

    private static final RpiScenarioSet SCENARIOS = new RpiScenarioSet(List.of(
            new RpiScenario("low",  LOW_DATASET),
            new RpiScenario("high", HIGH_DATASET)
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
        var mutable = new java.util.ArrayList<>(List.of(new RpiScenario("base", LOW_DATASET)));
        var set = new RpiScenarioSet(mutable);
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
