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
        Optional<YearMonth> drawdownStart,
        Optional<BigDecimal> monthlyDrawdown
) implements FinancialItem {

    @Override
    public NavigableMap<YearMonth, BigDecimal> positions(YearMonth from, YearMonth to) {
        NavigableMap<YearMonth, BigDecimal> result = new TreeMap<>();
        iterate(from, to, (month, pot, draw) -> result.put(month, pot));
        return result;
    }

    @Override
    public NavigableMap<YearMonth, BigDecimal> flows(YearMonth from, YearMonth to) {
        if (drawdownStart.isEmpty()) return new TreeMap<>();
        NavigableMap<YearMonth, BigDecimal> result = new TreeMap<>();
        iterate(from, to, (month, pot, draw) -> {
            if (draw.compareTo(BigDecimal.ZERO) > 0) result.put(month, draw);
        });
        return result;
    }

    private void iterate(YearMonth from, YearMonth to, PotConsumer consumer) {
        if (start.isAfter(to)) return;

        BigDecimal factor = BigDecimal.valueOf(Math.pow(1 + annualGrowthRate.doubleValue(), 1.0 / 12));
        BigDecimal pot = startValue;
        YearMonth current = start;

        while (!current.isAfter(to) && pot.compareTo(BigDecimal.ZERO) > 0) {
            // Position recorded before this month's growth and drawdown — opening balance
            BigDecimal positionThisMonth = pot;

            pot = pot.multiply(factor, MathContext.DECIMAL64);

            BigDecimal draw = BigDecimal.ZERO;
            if (drawdownStart.isPresent() && !current.isBefore(drawdownStart.get())) {
                draw = monthlyDrawdown.get().min(pot);
                pot = pot.subtract(draw).max(BigDecimal.ZERO);
            }

            if (!current.isBefore(from)) {
                consumer.accept(current, positionThisMonth, draw);
            }

            current = current.plusMonths(1);
        }
    }

    @FunctionalInterface
    private interface PotConsumer {
        void accept(YearMonth month, BigDecimal pot, BigDecimal draw);
    }
}
