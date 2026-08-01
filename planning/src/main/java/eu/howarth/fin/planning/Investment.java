package eu.howarth.fin.planning;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.YearMonth;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public record Investment(
        String name,
        String description,
        YearMonth start,
        BigDecimal startValue,
        BigDecimal annualGrowthRate,
        Optional<YearMonth> contributionStart,
        Optional<BigDecimal> monthlyContribution,
        BigDecimal contributionGrowthRate,
        Optional<YearMonth> contributionEnd,
        Optional<YearMonth> drawdownStart,
        Optional<BigDecimal> monthlyDrawdown,
        BigDecimal drawdownGrowthRate
) implements FinancialItem {

    @Override
    public NavigableMap<YearMonth, BigDecimal> positions(YearMonth from, YearMonth to) {
        NavigableMap<YearMonth, BigDecimal> result = new TreeMap<>();
        iterate(from, to, (month, pot, flow) -> result.put(month, pot));
        return result;
    }

    @Override
    public NavigableMap<YearMonth, BigDecimal> flows(YearMonth from, YearMonth to) {
        if (drawdownStart.isEmpty() && monthlyContribution.isEmpty()) return new TreeMap<>();
        NavigableMap<YearMonth, BigDecimal> result = new TreeMap<>();
        iterate(from, to, (month, pot, flow) -> {
            if (flow.compareTo(BigDecimal.ZERO) != 0) result.put(month, flow);
        });
        return result;
    }

    private void iterate(YearMonth from, YearMonth to, PotConsumer consumer) {
        if (start.isAfter(to)) return;

        BigDecimal potFactor = BigDecimal.valueOf(Math.pow(1 + annualGrowthRate.doubleValue(), 1.0 / 12));
        BigDecimal contributionFactor = BigDecimal.valueOf(Math.pow(1 + contributionGrowthRate.doubleValue(), 1.0 / 12));
        BigDecimal drawdownFactor = BigDecimal.valueOf(Math.pow(1 + drawdownGrowthRate.doubleValue(), 1.0 / 12));
        BigDecimal pot = startValue;
        BigDecimal contribution = monthlyContribution.orElse(BigDecimal.ZERO);
        BigDecimal drawdown = monthlyDrawdown.orElse(BigDecimal.ZERO);
        YearMonth effectiveContributionStart = contributionStart.orElse(start);
        YearMonth current = start;

        while (!current.isAfter(to) && pot.compareTo(BigDecimal.ZERO) > 0) {
            // Position recorded before this month's growth/contribution/drawdown — opening balance
            BigDecimal positionThisMonth = pot;

            pot = pot.multiply(potFactor, MathContext.DECIMAL64);

            // Drawdown always takes priority if contributionEnd and drawdownStart ever overlap.
            YearMonth month = current;
            boolean inDrawdown = drawdownStart.isPresent() && !month.isBefore(drawdownStart.get());
            boolean inContribution = !inDrawdown && monthlyContribution.isPresent()
                    && !month.isBefore(effectiveContributionStart)
                    && contributionEnd.map(e -> !month.isAfter(e)).orElse(true);

            BigDecimal flow = BigDecimal.ZERO;
            if (inDrawdown) {
                BigDecimal draw = drawdown.min(pot);
                pot = pot.subtract(draw).max(BigDecimal.ZERO);
                flow = draw;
            } else if (inContribution) {
                pot = pot.add(contribution);
                flow = contribution.negate();
            }

            if (!current.isBefore(from)) {
                consumer.accept(current, positionThisMonth, flow);
            }

            current = current.plusMonths(1);
            if (inContribution) contribution = contribution.multiply(contributionFactor, MathContext.DECIMAL64);
            if (inDrawdown) drawdown = drawdown.multiply(drawdownFactor, MathContext.DECIMAL64);
        }
    }

    @FunctionalInterface
    private interface PotConsumer {
        void accept(YearMonth month, BigDecimal pot, BigDecimal flow);
    }
}
