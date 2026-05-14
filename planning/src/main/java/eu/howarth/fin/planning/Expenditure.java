package eu.howarth.fin.planning;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public record Expenditure(
        String name,
        String description,
        YearMonth start,
        Optional<YearMonth> end,
        BigDecimal monthlyAmount
) implements FinancialItem {

    @Override
    public NavigableMap<YearMonth, BigDecimal> flows(YearMonth from, YearMonth to) {
        // absent end = one-off at start; present end = recurring start→end
        YearMonth effectiveEnd = end.orElse(start);

        YearMonth effectiveStart = start.isAfter(from) ? start : from;
        YearMonth clampedEnd     = effectiveEnd.isBefore(to) ? effectiveEnd : to;

        if (effectiveStart.isAfter(clampedEnd)) return new TreeMap<>();

        BigDecimal outflow = monthlyAmount.negate();
        NavigableMap<YearMonth, BigDecimal> result = new TreeMap<>();
        YearMonth current = effectiveStart;
        while (!current.isAfter(clampedEnd)) {
            result.put(current, outflow);
            current = current.plusMonths(1);
        }
        return result;
    }
}
