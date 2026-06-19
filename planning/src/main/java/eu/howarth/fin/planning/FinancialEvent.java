package eu.howarth.fin.planning;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * A one-off, dated cash movement — a single signed flow at one month.
 * Positive amount is money in (e.g. a bonus or inheritance), negative is money
 * out (e.g. a wedding or car). Unlike Income/Expenditure it has no lifecycle,
 * so a one-off is modelled here rather than as a degenerate recurring item.
 */
public record FinancialEvent(
        String name,
        String description,
        YearMonth date,
        BigDecimal amount
) implements FinancialItem {

    @Override
    public NavigableMap<YearMonth, BigDecimal> flows(YearMonth from, YearMonth to) {
        NavigableMap<YearMonth, BigDecimal> result = new TreeMap<>();
        if (!date.isBefore(from) && !date.isAfter(to)) {
            result.put(date, amount);
        }
        return result;
    }
}
