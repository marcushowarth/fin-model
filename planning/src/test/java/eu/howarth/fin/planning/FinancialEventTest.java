package eu.howarth.fin.planning;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class FinancialEventTest {

    private static final YearMonth JAN_2024 = YearMonth.of(2024, 1);
    private static final YearMonth MAY_2027 = YearMonth.of(2027, 5);
    private static final YearMonth DEC_2030 = YearMonth.of(2030, 12);

    // Money out — a one-off expense
    private static final FinancialEvent WEDDING = new FinancialEvent(
            "wedding", "daughters wedding", MAY_2027, bd("-15000"));

    // Money in — a one-off windfall
    private static final FinancialEvent INHERITANCE = new FinancialEvent(
            "inheritance", "estate", MAY_2027, bd("50000"));

    // --- Positions always empty ---

    @Test
    void positions_alwaysEmpty() {
        assertTrue(WEDDING.positions(JAN_2024, DEC_2030).isEmpty());
    }

    // --- Flows: single dated movement ---

    @Test
    void flows_atDate_singleEntry() {
        var flows = WEDDING.flows(JAN_2024, DEC_2030);
        assertEquals(1, flows.size());
        assertTrue(flows.containsKey(MAY_2027));
    }

    @Test
    void flows_moneyOut_isNegative() {
        var flows = WEDDING.flows(MAY_2027, MAY_2027);
        assertAmount("-15000", flows.get(MAY_2027));
    }

    @Test
    void flows_moneyIn_isPositive() {
        var flows = INHERITANCE.flows(MAY_2027, MAY_2027);
        assertAmount("50000", flows.get(MAY_2027));
    }

    @Test
    void flows_beforeDate_returnsEmpty() {
        assertTrue(WEDDING.flows(JAN_2024, YearMonth.of(2027, 4)).isEmpty());
    }

    @Test
    void flows_afterDate_returnsEmpty() {
        assertTrue(WEDDING.flows(YearMonth.of(2027, 6), DEC_2030).isEmpty());
    }

    @Test
    void flows_dateOutsideRange_returnsEmpty() {
        assertTrue(WEDDING.flows(JAN_2024, YearMonth.of(2025, 1)).isEmpty());
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Expected " + expected + " but was " + actual);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
