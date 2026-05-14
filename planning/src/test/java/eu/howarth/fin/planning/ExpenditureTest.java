package eu.howarth.fin.planning;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExpenditureTest {

    private static final YearMonth JAN_2024 = YearMonth.of(2024, 1);
    private static final YearMonth DEC_2030 = YearMonth.of(2030, 12);
    private static final YearMonth MAY_2027 = YearMonth.of(2027, 5);

    // Recurring — monthly bills with end date
    private static final Expenditure BILLS = new Expenditure(
            "bills", "household bills", JAN_2024, Optional.of(DEC_2030), bd("2000"));

    // One-off — no end date
    private static final Expenditure WEDDING = new Expenditure(
            "wedding", "daughters wedding", MAY_2027, Optional.empty(), bd("15000"));

    // --- Positions always empty ---

    @Test
    void positions_alwaysEmpty() {
        assertTrue(BILLS.positions(JAN_2024, DEC_2030).isEmpty());
    }

    // --- Flows: structural ---

    @Test
    void flows_atStartMonth_returnsAmount() {
        var flows = BILLS.flows(JAN_2024, JAN_2024);
        assertEquals(1, flows.size());
        assertAmount("-2000", flows.get(JAN_2024));
    }

    @Test
    void flows_allNegative() {
        BILLS.flows(JAN_2024, DEC_2030).values()
                .forEach(v -> assertTrue(v.compareTo(BigDecimal.ZERO) < 0, "Expenditure flows must be negative"));
    }

    @Test
    void flows_beforeStart_returnsEmpty() {
        var flows = BILLS.flows(YearMonth.of(2020, 1), YearMonth.of(2023, 12));
        assertTrue(flows.isEmpty());
    }

    @Test
    void flows_afterEnd_returnsEmpty() {
        var flows = BILLS.flows(YearMonth.of(2031, 1), YearMonth.of(2035, 1));
        assertTrue(flows.isEmpty());
    }

    @Test
    void flows_atEndMonth_includesEndMonth() {
        var flows = BILLS.flows(DEC_2030, DEC_2030);
        assertEquals(1, flows.size());
        assertAmount("-2000", flows.get(DEC_2030));
    }

    @Test
    void flows_rangeCoversLifetime_containsOneEntryPerMonth() {
        var flows = BILLS.flows(JAN_2024, DEC_2030);
        long expectedMonths = JAN_2024.until(DEC_2030, ChronoUnit.MONTHS) + 1;
        assertEquals(expectedMonths, flows.size());
    }

    @Test
    void flows_constantAmount_allMonthsSameValue() {
        BILLS.flows(JAN_2024, YearMonth.of(2024, 6)).values()
                .forEach(v -> assertAmount("-2000", v));
    }

    // --- One-off ---

    @Test
    void flows_oneOff_noEnd_singleFlowAtStart() {
        var flows = WEDDING.flows(YearMonth.of(2025, 1), YearMonth.of(2030, 1));
        assertEquals(1, flows.size());
        assertTrue(flows.containsKey(MAY_2027));
    }

    @Test
    void flows_oneOff_correctAmount() {
        var flows = WEDDING.flows(MAY_2027, MAY_2027);
        assertAmount("-15000", flows.get(MAY_2027));
    }

    @Test
    void flows_oneOff_beforeStart_returnsEmpty() {
        var flows = WEDDING.flows(YearMonth.of(2025, 1), YearMonth.of(2026, 12));
        assertTrue(flows.isEmpty());
    }

    @Test
    void flows_oneOff_afterStart_returnsEmpty() {
        var flows = WEDDING.flows(YearMonth.of(2027, 6), YearMonth.of(2030, 1));
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
