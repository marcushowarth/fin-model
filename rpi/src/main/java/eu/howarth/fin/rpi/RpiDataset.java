package eu.howarth.fin.rpi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record RpiDataset(List<RpiEntry> entries) {

    public RpiDataset {
        entries = List.copyOf(entries);  // compact constructor — enforces immutability
    }

    public BigDecimal indexForMonth(int year, int month) {
        return entries.stream()
                .filter(e -> e.year() == year && e.month() == month)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No RPI data for " + year + "-" + month))
                .index();
    }

    public BigDecimal indexForYear(int year) {
        List<BigDecimal> monthly = entries.stream()
                .filter(e -> e.year() == year)
                .map(RpiEntry::index)
                .toList();
        if (monthly.isEmpty()) throw new IllegalArgumentException("No RPI data for year: " + year);
        BigDecimal sum = monthly.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(monthly.size()), 10, RoundingMode.HALF_UP);
    }
}

