package eu.howarth.fin.planning;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IncomeTest {

    private static final YearMonth JAN_2024 = YearMonth.of(2024, 1);
    private static final YearMonth DEC_2030 = YearMonth.of(2030, 12);

    // Salary with end date, no growth
    private static final Income SALARY = new Income(
            "salary", "monthly pay", JAN_2024, Optional.of(DEC_2030), bd("5000"), BigDecimal.ZERO);

    // Growing salary — 3% annual
    private static final Income GROWING = new Income(
            "salary", "monthly pay", JAN_2024, Optional.of(DEC_2030), bd("5000"), bd("0.03"));

    // State pension — no end date
    private static final Income PENSION = new Income(
            "state pension", "government pension", YearMonth.of(2030, 1), Optional.empty(), bd("900"), BigDecimal.ZERO);

    // --- Positions always empty ---

    @Test
    void positions_alwaysEmpty() {
        assertTrue(SALARY.positions(JAN_2024, DEC_2030).isEmpty());
    }

    // --- Flows: structural ---

    @Test
    void flows_atStartMonth_returnsMonthlyAmount() {
        var flows = SALARY.flows(JAN_2024, JAN_2024);
        assertEquals(1, flows.size());
        assertAmount("5000", flows.get(JAN_2024));
    }

    @Test
    void flows_beforeStart_returnsEmpty() {
        var flows = SALARY.flows(YearMonth.of(2020, 1), YearMonth.of(2023, 12));
        assertTrue(flows.isEmpty());
    }

    @Test
    void flows_afterEnd_returnsEmpty() {
        var flows = SALARY.flows(YearMonth.of(2031, 1), YearMonth.of(2035, 1));
        assertTrue(flows.isEmpty());
    }

    @Test
    void flows_atEndMonth_includesEndMonth() {
        var flows = SALARY.flows(DEC_2030, DEC_2030);
        assertEquals(1, flows.size());
        assertAmount("5000", flows.get(DEC_2030));
    }

    @Test
    void flows_rangeCoversLifetime_containsOneEntryPerMonth() {
        var flows = SALARY.flows(JAN_2024, DEC_2030);
        // Jan 2024 to Dec 2030 inclusive
        long expectedMonths = JAN_2024.until(DEC_2030, java.time.temporal.ChronoUnit.MONTHS) + 1;
        assertEquals(expectedMonths, flows.size());
    }

    @Test
    void flows_noEndDate_extendsToQueryTo() {
        var flows = PENSION.flows(YearMonth.of(2030, 1), YearMonth.of(2040, 12));
        assertFalse(flows.isEmpty());
        assertTrue(flows.containsKey(YearMonth.of(2040, 12)));
    }

    @Test
    void flows_noEndDate_doesNotExceedQueryTo() {
        var flows = PENSION.flows(YearMonth.of(2030, 1), YearMonth.of(2035, 6));
        assertFalse(flows.containsKey(YearMonth.of(2035, 7)));
    }

    @Test
    void flows_allPositive() {
        SALARY.flows(JAN_2024, DEC_2030).values()
                .forEach(v -> assertTrue(v.compareTo(BigDecimal.ZERO) > 0));
    }

    // --- Flows: growth ---

    @Test
    void flows_zeroRate_allMonthsSameAmount() {
        SALARY.flows(JAN_2024, YearMonth.of(2024, 6)).values()
                .forEach(v -> assertAmount("5000", v));
    }

    @Test
    void flows_positiveRate_amountIncreasesEachMonth() {
        var flows = GROWING.flows(JAN_2024, YearMonth.of(2025, 1));
        var months = flows.sequencedValues().stream().toList();
        for (int i = 1; i < months.size(); i++) {
            assertTrue(months.get(i).compareTo(months.get(i - 1)) > 0,
                    "Amount should increase each month");
        }
    }

    @Test
    void flows_positiveRate_afterOneYear_approximatesAnnualRate() {
        // £5000/month at 3% annual → ~£5150 after 12 months
        var flows = GROWING.flows(JAN_2024, YearMonth.of(2025, 1));
        BigDecimal yearEnd = flows.get(YearMonth.of(2025, 1));
        assertTrue(yearEnd.compareTo(bd("5148")) > 0 && yearEnd.compareTo(bd("5152")) < 0,
                () -> "Expected ~5150, got " + yearEnd);
    }

    @Test
    void flows_queryStartsAfterIncomeStart_correctAmountAtQueryStart() {
        // Query starts mid-income — amount should reflect growth since income start
        var flows = GROWING.flows(YearMonth.of(2025, 1), YearMonth.of(2025, 1));
        BigDecimal amount = flows.get(YearMonth.of(2025, 1));
        // 12 months of 3% growth from £5000
        assertTrue(amount.compareTo(bd("5148")) > 0 && amount.compareTo(bd("5152")) < 0,
                () -> "Expected ~5150, got " + amount);
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Expected " + expected + " but was " + actual);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
