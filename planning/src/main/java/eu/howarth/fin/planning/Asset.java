package eu.howarth.fin.planning;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public record Asset(
        String name,
        String description,
        YearMonth start,
        BigDecimal startValue,
        BigDecimal annualGrowthRate,
        Optional<YearMonth> saleDate
) implements FinancialItem {

    @Override
    public NavigableMap<YearMonth, BigDecimal> positions(YearMonth from, YearMonth to) {
        YearMonth effectiveStart = start.isAfter(from) ? start : from;
        // Positions end the month before sale — on sale date the asset is converted to cash
        YearMonth posEnd = saleDate.map(sd -> sd.minusMonths(1)).orElse(to);
        YearMonth effectiveEnd = posEnd.isBefore(to) ? posEnd : to;

        if (effectiveStart.isAfter(effectiveEnd)) return new TreeMap<>();

        double monthlyFactor = monthlyFactor();
        long monthsFromStart = start.until(effectiveStart, ChronoUnit.MONTHS);

        BigDecimal value = startValue
                .multiply(BigDecimal.valueOf(Math.pow(monthlyFactor, monthsFromStart)), MathContext.DECIMAL64)
                .max(BigDecimal.ZERO);

        NavigableMap<YearMonth, BigDecimal> result = new TreeMap<>();
        YearMonth current = effectiveStart;
        while (!current.isAfter(effectiveEnd)) {
            result.put(current, value);
            current = current.plusMonths(1);
            if (!current.isAfter(effectiveEnd)) {
                value = value.multiply(BigDecimal.valueOf(monthlyFactor), MathContext.DECIMAL64)
                        .max(BigDecimal.ZERO);
            }
        }
        return result;
    }

    @Override
    public NavigableMap<YearMonth, BigDecimal> flows(YearMonth from, YearMonth to) {
        if (saleDate.isEmpty()) return new TreeMap<>();
        YearMonth sd = saleDate.get();
        if (sd.isBefore(from) || sd.isAfter(to)) return new TreeMap<>();

        NavigableMap<YearMonth, BigDecimal> result = new TreeMap<>();
        result.put(sd, valueAt(sd));
        return result;
    }

    private BigDecimal valueAt(YearMonth target) {
        long months = start.until(target, ChronoUnit.MONTHS);
        return startValue
                .multiply(BigDecimal.valueOf(Math.pow(monthlyFactor(), months)), MathContext.DECIMAL64)
                .max(BigDecimal.ZERO);
    }

    private double monthlyFactor() {
        return Math.pow(1 + annualGrowthRate.doubleValue(), 1.0 / 12);
    }
}
