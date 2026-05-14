package eu.howarth.fin.rpi.projection;

import eu.howarth.fin.rpi.RpiDataset;
import eu.howarth.fin.rpi.RpiEntry;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RpiProjector {

    public static RpiDataset project(RpiDataset historical, InflationProjection projection, YearMonth upTo) {
        RpiEntry anchor = historical.entries().stream()
                .max(Comparator.comparing(e -> YearMonth.of(e.year(), e.month())))
                .orElseThrow(() -> new IllegalArgumentException("Historical dataset is empty"));

        List<RpiEntry> combined = new ArrayList<>(historical.entries());
        combined.addAll(projection.project(anchor, upTo));
        return new RpiDataset(combined);
    }
}
