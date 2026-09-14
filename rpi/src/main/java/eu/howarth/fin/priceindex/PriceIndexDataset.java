package eu.howarth.fin.priceindex;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * A single published index's monthly readings, tagged with which
 * {@link IndexSeries} they belong to — the multi-index generalisation of
 * what {@code RpiDataset} did for RPI alone (kanban #981).
 */
public record PriceIndexDataset(IndexSeries series, List<PriceIndexEntry> entries) {

    public PriceIndexDataset {
        entries = List.copyOf(entries);  // compact constructor — enforces immutability
    }

    public BigDecimal indexForMonth(int year, int month) {
        return entries.stream()
                .filter(e -> e.year() == year && e.month() == month)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No " + series + " data for " + year + "-" + month))
                .index();
    }

    public BigDecimal indexForYear(int year) {
        List<BigDecimal> monthly = entries.stream()
                .filter(e -> e.year() == year)
                .map(PriceIndexEntry::index)
                .toList();
        if (monthly.isEmpty()) throw new IllegalArgumentException("No " + series + " data for year: " + year);
        BigDecimal sum = monthly.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(monthly.size()), 10, RoundingMode.HALF_UP);
    }
}
