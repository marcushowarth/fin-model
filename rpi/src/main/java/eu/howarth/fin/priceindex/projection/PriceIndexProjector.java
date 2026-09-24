package eu.howarth.fin.priceindex.projection;

import eu.howarth.fin.priceindex.PriceIndexDataset;
import eu.howarth.fin.priceindex.PriceIndexEntry;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PriceIndexProjector {

    public static PriceIndexDataset project(PriceIndexDataset historical, IndexProjection projection, YearMonth upTo) {
        PriceIndexEntry anchor = historical.entries().stream()
                .max(Comparator.comparing(e -> YearMonth.of(e.year(), e.month())))
                .orElseThrow(() -> new IllegalArgumentException("Historical dataset is empty"));

        List<PriceIndexEntry> combined = new ArrayList<>(historical.entries());
        combined.addAll(projection.project(anchor, upTo));
        return new PriceIndexDataset(historical.series(), combined);
    }
}
