package eu.howarth.fin.planning;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class LiabilityTest {

    private static final YearMonth JAN_2024 = YearMonth.of(2024, 1);
    private static final YearMonth FEB_2024 = YearMonth.of(2024, 2);
    private static final YearMonth JAN_2025 = YearMonth.of(2025, 1);

    // balance=1200, repayment=100, rate=0% → exactly 12 months (Jan–Dec 2024)
    private static final Liability ZERO_RATE = new Liability(
            "car loan", "car loan", JAN_2024, bd("1200"), BigDecimal.ZERO, bd("100"));

    // balance=150, repayment=100, rate=0% → 2 months: £100 then £50 (clips)
    private static final Liability UNEVEN = new Liability(
            "short loan", "short loan", JAN_2024, bd("150"), BigDecimal.ZERO, bd("100"));

    // Realistic mortgage — balance reduces, interest slows repayment
    private static final Liability MORTGAGE = new Liability(
            "mortgage", "home mortgage", JAN_2024, bd("200000"), bd("0.035"), bd("900"));

    // --- Positions ---

    @Test
    void positions_atStart_returnsNegativeInitialBalance() {
        var pos = ZERO_RATE.positions(JAN_2024, JAN_2024);
        assertAmount("-1200", pos.get(JAN_2024));
    }

    @Test
    void positions_beforeStart_returnsEmpty() {
        assertTrue(ZERO_RATE.positions(YearMonth.of(2020, 1), YearMonth.of(2023, 12)).isEmpty());
    }

    @Test
    void positions_zeroRate_secondMonth_reducedByOneRepayment() {
        var pos = ZERO_RATE.positions(FEB_2024, FEB_2024);
        assertAmount("-1100", pos.get(FEB_2024));
    }

    @Test
    void positions_zeroRate_becomesLessNegativeEachMonth() {
        var pos = ZERO_RATE.positions(JAN_2024, YearMonth.of(2024, 12));
        var months = pos.sequencedValues().stream().toList();
        for (int i = 1; i < months.size(); i++) {
            assertTrue(months.get(i).compareTo(months.get(i - 1)) > 0,
                    "Position should become less negative each month");
        }
    }

    @Test
    void positions_zeroRate_fullyRepaidAfterTerm() {
        // 1200 / 100 = 12 months — Jan 2025 should not be present
        var pos = ZERO_RATE.positions(JAN_2024, YearMonth.of(2025, 6));
        assertFalse(pos.containsKey(JAN_2025), "No position once fully repaid");
        assertTrue(pos.containsKey(YearMonth.of(2024, 12)), "Final month still has position");
    }

    @Test
    void positions_withInterest_moreNegativeThanZeroRate_atSameDate() {
        // Interest slows repayment — balance higher (more negative) than zero-rate equivalent
        var zeroPos     = ZERO_RATE.positions(FEB_2024, FEB_2024).get(FEB_2024);
        var mortgagePos = MORTGAGE.positions(FEB_2024, FEB_2024).get(FEB_2024);
        // Both start at different balances so compare directionally: mortgage balance reduces slower
        // At month 2: zero_rate balance went 1200→1100, mortgage ~200000→199677 (interest slows it)
        // Mortgage position is more negative than -(200000 - 900) = -199100
        assertTrue(mortgagePos.compareTo(bd("-199100")) < 0,
                "With interest, less than one full repayment comes off the balance");
    }

    @Test
    void positions_allNegative() {
        ZERO_RATE.positions(JAN_2024, YearMonth.of(2024, 12)).values()
                .forEach(v -> assertTrue(v.compareTo(BigDecimal.ZERO) < 0));
    }

    // --- Flows ---

    @Test
    void flows_atStart_returnsNegativeRepayment() {
        var flows = ZERO_RATE.flows(JAN_2024, JAN_2024);
        assertAmount("-100", flows.get(JAN_2024));
    }

    @Test
    void flows_beforeStart_returnsEmpty() {
        assertTrue(ZERO_RATE.flows(YearMonth.of(2020, 1), YearMonth.of(2023, 12)).isEmpty());
    }

    @Test
    void flows_allNegative() {
        ZERO_RATE.flows(JAN_2024, YearMonth.of(2024, 12)).values()
                .forEach(v -> assertTrue(v.compareTo(BigDecimal.ZERO) < 0));
    }

    @Test
    void flows_zeroRate_exactTermLength() {
        // 12 payments of £100 to clear £1200 at 0%
        var flows = ZERO_RATE.flows(JAN_2024, YearMonth.of(2025, 6));
        assertEquals(12, flows.size());
    }

    @Test
    void flows_stopsAfterFullyRepaid() {
        var flows = ZERO_RATE.flows(JAN_2024, YearMonth.of(2025, 6));
        assertFalse(flows.containsKey(JAN_2025));
    }

    @Test
    void flows_finalPaymentClipsToRemainingBalance() {
        // balance=150, repayment=100 → month 1: -100, month 2: -50
        var flows = UNEVEN.flows(JAN_2024, YearMonth.of(2024, 6));
        assertEquals(2, flows.size());
        assertAmount("-100", flows.get(JAN_2024));
        assertAmount("-50",  flows.get(FEB_2024));
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Expected " + expected + " but was " + actual);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
