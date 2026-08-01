package eu.howarth.fin.planning;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InvestmentTest {

    private static final YearMonth JAN_2024 = YearMonth.of(2024, 1);
    private static final YearMonth JAN_2034 = YearMonth.of(2034, 1);

    // Pure growth — no contribution, no drawdown
    private static final Investment SIPP_GROWING = new Investment(
            "SIPP", "self-invested pension", JAN_2024, new BigDecimal("200000"),
            new BigDecimal("0.05"), Optional.empty(), Optional.empty(), BigDecimal.ZERO, Optional.empty(),
            Optional.empty(), Optional.empty(), BigDecimal.ZERO);

    // Zero growth, no contribution, no drawdown — flat position
    private static final Investment STATIC = new Investment(
            "ISA", "stocks and shares ISA", JAN_2024, new BigDecimal("50000"),
            BigDecimal.ZERO, Optional.empty(), Optional.empty(), BigDecimal.ZERO, Optional.empty(),
            Optional.empty(), Optional.empty(), BigDecimal.ZERO);

    // Drawdown from Jan 2034, £800/month, 5% growth, no contribution
    private static final Investment SIPP_DRAWDOWN = new Investment(
            "SIPP", "self-invested pension", JAN_2024, new BigDecimal("200000"),
            new BigDecimal("0.05"), Optional.empty(), Optional.empty(), BigDecimal.ZERO, Optional.empty(),
            Optional.of(JAN_2034), Optional.of(new BigDecimal("800")), BigDecimal.ZERO);

    // £500/month contribution, 3% contribution growth, no end, no drawdown — accumulation phase only
    private static final Investment SIPP_CONTRIBUTING = new Investment(
            "SIPP", "self-invested pension", JAN_2024, new BigDecimal("200000"),
            new BigDecimal("0.05"), Optional.empty(), Optional.of(new BigDecimal("500")), new BigDecimal("0.03"),
            Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.ZERO);

    // Contributions Jan 2024 - Dec 2028 (contributionEnd), gap, drawdown from Jan 2034
    private static final YearMonth DEC_2028 = YearMonth.of(2028, 12);
    private static final Investment SIPP_WITH_GAP = new Investment(
            "SIPP", "self-invested pension", JAN_2024, new BigDecimal("200000"),
            new BigDecimal("0.05"), Optional.empty(), Optional.of(new BigDecimal("500")), BigDecimal.ZERO,
            Optional.of(DEC_2028), Optional.of(JAN_2034), Optional.of(new BigDecimal("800")), BigDecimal.ZERO);

    // --- Positions: no drawdown ---

    @Test
    void positions_atStart_returnsStartValue() {
        var pos = STATIC.positions(JAN_2024, JAN_2024);
        assertEquals(0, new BigDecimal("50000").compareTo(pos.get(JAN_2024)));
    }

    @Test
    void positions_beforeStart_returnsEmpty() {
        assertTrue(STATIC.positions(YearMonth.of(2020, 1), YearMonth.of(2023, 12)).isEmpty());
    }

    @Test
    void positions_zeroRate_noDrawdown_remainsConstant() {
        STATIC.positions(JAN_2024, YearMonth.of(2024, 6)).values()
                .forEach(v -> assertEquals(0, new BigDecimal("50000").compareTo(v)));
    }

    @Test
    void positions_positiveRate_noDrawdown_growsEachMonth() {
        var pos = SIPP_GROWING.positions(JAN_2024, YearMonth.of(2025, 1));
        var months = pos.sequencedValues().stream().toList();
        for (int i = 1; i < months.size(); i++) {
            assertTrue(months.get(i).compareTo(months.get(i - 1)) > 0, "Position should grow each month");
        }
    }

    @Test
    void positions_positiveRate_afterOneYear_approximatesAnnualRate() {
        // £200,000 at 5% → ~£210,000 after 12 months
        var pos = SIPP_GROWING.positions(JAN_2024, YearMonth.of(2025, 1));
        BigDecimal yearEnd = pos.get(YearMonth.of(2025, 1));
        assertTrue(yearEnd.compareTo(new BigDecimal("209800")) > 0
                && yearEnd.compareTo(new BigDecimal("210200")) < 0,
                () -> "Expected ~210000, got " + yearEnd);
    }

    // --- Positions: with drawdown ---

    @Test
    void positions_beforeDrawdown_growsNormally() {
        // Before drawdown starts, position should grow as if no drawdown
        var withDrawdown    = SIPP_DRAWDOWN.positions(JAN_2024, YearMonth.of(2033, 12));
        var withoutDrawdown = SIPP_GROWING.positions(JAN_2024, YearMonth.of(2033, 12));
        assertEquals(0, withDrawdown.get(YearMonth.of(2033, 12))
                .compareTo(withoutDrawdown.get(YearMonth.of(2033, 12))));
    }

    @Test
    void positions_drawdownReducesPotEachMonth() {
        // From Feb 2034 onwards — drawdown taken in Jan only shows in Feb's opening balance
        var pos            = SIPP_DRAWDOWN.positions(YearMonth.of(2034, 2), YearMonth.of(2035, 1));
        var noDrawdownPos  = SIPP_GROWING.positions(YearMonth.of(2034, 2), YearMonth.of(2035, 1));
        var months         = pos.sequencedValues().stream().toList();
        var noDrawdownMonths = noDrawdownPos.sequencedValues().stream().toList();
        for (int i = 0; i < months.size(); i++) {
            assertTrue(months.get(i).compareTo(noDrawdownMonths.get(i)) < 0,
                    "Drawdown should reduce opening balance vs no-drawdown from the month after first draw");
        }
    }

    @Test
    void positions_potExhausted_noFurtherPositions() {
        // Small pot, large drawdown, zero growth → pot exhausted quickly
        var exhausting = new Investment("small", "small pot", JAN_2024, new BigDecimal("500"),
                BigDecimal.ZERO, Optional.empty(), Optional.empty(), BigDecimal.ZERO, Optional.empty(),
                Optional.of(JAN_2024), Optional.of(new BigDecimal("200")), BigDecimal.ZERO);
        var pos = exhausting.positions(JAN_2024, YearMonth.of(2030, 1));
        // 500 / 200 = 2.5 → 3 months (500, 300, 100) then exhausted
        assertEquals(3, pos.size());
    }

    // --- Flows: no drawdown, no contribution ---

    @Test
    void flows_noDrawdownNoContribution_alwaysEmpty() {
        assertTrue(SIPP_GROWING.flows(JAN_2024, YearMonth.of(2030, 1)).isEmpty());
    }

    // --- Flows: with drawdown ---

    @Test
    void flows_beforeDrawdownStart_empty() {
        var flows = SIPP_DRAWDOWN.flows(JAN_2024, YearMonth.of(2033, 12));
        assertTrue(flows.isEmpty());
    }

    @Test
    void flows_atDrawdownStart_returnsMonthlyDrawdown() {
        var flows = SIPP_DRAWDOWN.flows(JAN_2034, JAN_2034);
        assertEquals(1, flows.size());
        assertEquals(0, new BigDecimal("800").compareTo(flows.get(JAN_2034)));
    }

    @Test
    void flows_allDrawdownPositive() {
        SIPP_DRAWDOWN.flows(JAN_2034, YearMonth.of(2040, 1)).values()
                .forEach(v -> assertTrue(v.compareTo(BigDecimal.ZERO) > 0, "Drawdown flows must be positive"));
    }

    @Test
    void flows_potExhausted_stopsWhenPotEmpty() {
        var exhausting = new Investment("small", "small pot", JAN_2024, new BigDecimal("500"),
                BigDecimal.ZERO, Optional.empty(), Optional.empty(), BigDecimal.ZERO, Optional.empty(),
                Optional.of(JAN_2024), Optional.of(new BigDecimal("200")), BigDecimal.ZERO);
        var flows = exhausting.flows(JAN_2024, YearMonth.of(2030, 1));
        // 500 → pay 200, 300 → pay 200, 100 → pay 100 (clips) = 3 payments
        assertEquals(3, flows.size());
    }

    @Test
    void flows_finalDrawdownClipsToRemainingPot() {
        var exhausting = new Investment("small", "small pot", JAN_2024, new BigDecimal("500"),
                BigDecimal.ZERO, Optional.empty(), Optional.empty(), BigDecimal.ZERO, Optional.empty(),
                Optional.of(JAN_2024), Optional.of(new BigDecimal("200")), BigDecimal.ZERO);
        var flows = exhausting.flows(JAN_2024, YearMonth.of(2030, 1));
        // Month 3 (Mar 2024): remaining pot = 100, draws only 100 not 200
        assertEquals(0, new BigDecimal("100").compareTo(flows.get(YearMonth.of(2024, 3))));
    }

    @Test
    void flows_drawdownCompoundsAtOwnGrowthRate() {
        // 12% annual drawdown growth compounded monthly — later draws should be larger
        var growingDrawdown = new Investment("SIPP", "", JAN_2024, new BigDecimal("1000000"),
                BigDecimal.ZERO, Optional.empty(), Optional.empty(), BigDecimal.ZERO, Optional.empty(),
                Optional.of(JAN_2024), Optional.of(new BigDecimal("1000")), new BigDecimal("0.12"));
        var flows = growingDrawdown.flows(JAN_2024, YearMonth.of(2025, 1));
        var jan = flows.get(JAN_2024);
        var feb2025 = flows.get(YearMonth.of(2025, 1));
        assertTrue(feb2025.compareTo(jan) > 0, () -> "Expected " + feb2025 + " > " + jan);
    }

    // --- Flows: with contribution ---

    @Test
    void flows_contribution_isNegative() {
        SIPP_CONTRIBUTING.flows(JAN_2024, YearMonth.of(2025, 1)).values()
                .forEach(v -> assertTrue(v.compareTo(BigDecimal.ZERO) < 0, "Contribution flows must be negative"));
    }

    @Test
    void flows_contribution_firstMonthMatchesMonthlyContribution() {
        var flows = SIPP_CONTRIBUTING.flows(JAN_2024, JAN_2024);
        assertEquals(0, new BigDecimal("-500").compareTo(flows.get(JAN_2024)));
    }

    @Test
    void flows_contribution_compoundsAtOwnGrowthRate() {
        var flows = SIPP_CONTRIBUTING.flows(JAN_2024, YearMonth.of(2025, 1));
        var jan = flows.get(JAN_2024).abs();
        var feb2025 = flows.get(YearMonth.of(2025, 1)).abs();
        assertTrue(feb2025.compareTo(jan) > 0, () -> "Expected " + feb2025 + " > " + jan + " (3% annual growth)");
    }

    @Test
    void positions_contribution_growsPotFasterThanGrowthAlone() {
        var withContribution = SIPP_CONTRIBUTING.positions(JAN_2024, YearMonth.of(2025, 1));
        var withoutContribution = SIPP_GROWING.positions(JAN_2024, YearMonth.of(2025, 1));
        assertTrue(withContribution.get(YearMonth.of(2025, 1)).compareTo(withoutContribution.get(YearMonth.of(2025, 1))) > 0,
                "Contributions should grow the pot beyond investment growth alone");
    }

    // --- Flows: contributionStart (a gap BEFORE contributions begin) ---

    @Test
    void flows_beforeContributionStart_noFlow() {
        // Investment starts Jan 2024, but contributions don't begin until Jan 2026
        var delayed = new Investment("SIPP", "", JAN_2024, new BigDecimal("200000"),
                new BigDecimal("0.05"), Optional.of(YearMonth.of(2026, 1)), Optional.of(new BigDecimal("500")),
                BigDecimal.ZERO, Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.ZERO);
        var flows = delayed.flows(JAN_2024, YearMonth.of(2025, 12));
        assertTrue(flows.isEmpty(), "No contribution flow before contributionStart");
    }

    @Test
    void flows_atContributionStart_beginsFlowing() {
        var delayed = new Investment("SIPP", "", JAN_2024, new BigDecimal("200000"),
                new BigDecimal("0.05"), Optional.of(YearMonth.of(2026, 1)), Optional.of(new BigDecimal("500")),
                BigDecimal.ZERO, Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.ZERO);
        var flows = delayed.flows(YearMonth.of(2026, 1), YearMonth.of(2026, 1));
        assertEquals(0, new BigDecimal("-500").compareTo(flows.get(YearMonth.of(2026, 1))));
    }

    @Test
    void flows_contributionStartUnset_defaultsToItemStart() {
        // SIPP_CONTRIBUTING has no contributionStart set — should behave exactly as before (flows from `start`)
        var flows = SIPP_CONTRIBUTING.flows(JAN_2024, JAN_2024);
        assertTrue(flows.containsKey(JAN_2024), "Contribution should flow from item start when contributionStart is unset");
    }

    // --- Flows: contribution + gap + drawdown ---

    @Test
    void flows_gapPhase_noFlow() {
        // Between contributionEnd (Dec 2028) and drawdownStart (Jan 2034) — pure growth, no flow
        var flows = SIPP_WITH_GAP.flows(YearMonth.of(2029, 1), YearMonth.of(2033, 12));
        assertTrue(flows.isEmpty(), "Gap phase should produce no flow");
    }

    @Test
    void flows_contributionPhase_stopsAtContributionEnd() {
        var flows = SIPP_WITH_GAP.flows(JAN_2024, DEC_2028);
        assertTrue(flows.containsKey(DEC_2028), "Contribution should still apply on contributionEnd itself (inclusive)");
        assertTrue(flows.get(DEC_2028).compareTo(BigDecimal.ZERO) < 0);
    }

    @Test
    void flows_drawdownPhase_afterGap_stillPositive() {
        var flows = SIPP_WITH_GAP.flows(JAN_2034, JAN_2034);
        assertEquals(1, flows.size());
        assertEquals(0, new BigDecimal("800").compareTo(flows.get(JAN_2034)));
    }

    @Test
    void flows_drawdownTakesPriority_whenContributionEndOverlapsDrawdownStart() {
        // contributionEnd set AFTER drawdownStart — drawdown must still win from drawdownStart onward
        var overlapping = new Investment("SIPP", "", JAN_2024, new BigDecimal("200000"),
                new BigDecimal("0.05"), Optional.empty(), Optional.of(new BigDecimal("500")), BigDecimal.ZERO,
                Optional.of(YearMonth.of(2035, 1)), Optional.of(JAN_2034), Optional.of(new BigDecimal("800")), BigDecimal.ZERO);
        var flows = overlapping.flows(JAN_2034, JAN_2034);
        assertEquals(0, new BigDecimal("800").compareTo(flows.get(JAN_2034)),
                "Drawdown should win even though contributionEnd is later");
    }
}
