package eu.howarth.fin.planning;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public record Income(
        String name,
        String description,
        YearMonth start,
        Optional<YearMonth> end,
        BigDecimal monthlyAmount,
        BigDecimal annualGrowthRate
) implements FinancialItem {

    @Override
    public NavigableMap<YearMonth, BigDecimal> flows(YearMonth from, YearMonth to) {
        YearMonth effectiveStart = start.isAfter(from) ? start : from;
        YearMonth effectiveEnd   = end.map(e -> e.isBefore(to) ? e : to).orElse(to);

        if (effectiveStart.isAfter(effectiveEnd)) return new TreeMap<>();

        double monthlyFactor = Math.pow(1 + annualGrowthRate.doubleValue(), 1.0 / 12);
        long monthsFromStart = start.until(effectiveStart, ChronoUnit.MONTHS);

        BigDecimal amount = monthlyAmount
                .multiply(BigDecimal.valueOf(Math.pow(monthlyFactor, monthsFromStart)), MathContext.DECIMAL64);

        NavigableMap<YearMonth, BigDecimal> result = new TreeMap<>();
        YearMonth current = effectiveStart;
        while (!current.isAfter(effectiveEnd)) {
            result.put(current, amount);
            current = current.plusMonths(1);
            if (!current.isAfter(effectiveEnd)) {
                amount = amount.multiply(BigDecimal.valueOf(monthlyFactor), MathContext.DECIMAL64);
            }
        }
        return result;
    }
}
