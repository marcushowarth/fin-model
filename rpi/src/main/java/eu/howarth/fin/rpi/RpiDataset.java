package eu.howarth.fin.rpi;

import java.math.BigDecimal;
import java.util.List;

public record RpiDataset(List<RpiEntry> entries) {

    public RpiDataset {
        entries = List.copyOf(entries);  // compact constructor — enforces immutability
    }

    public BigDecimal indexForYear(int year) {
        return entries.stream()
                .filter(e -> e.year() == year)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No RPI data for year: " + year))
                .index();
    }
}

