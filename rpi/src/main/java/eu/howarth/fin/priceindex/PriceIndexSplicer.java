package eu.howarth.fin.priceindex;

import java.util.ArrayList;
import java.util.List;

/**
 * Combines a static historical series (e.g. {@link IndexSeries#RPI_MILLENNIUM})
 * with its live counterpart ({@link IndexSeries#splicesInto()}) into one
 * continuous dataset reaching to the present day. For any year present in
 * both, the live series wins — it's the more authoritative, weekly-refreshed
 * figure; the historical series only fills in years strictly before the live
 * series' own earliest year.
 */
public class PriceIndexSplicer {

    public static PriceIndexDataset spliceToPresent(IndexSeries series) {
        IndexSeries liveSeries = series.splicesInto();
        if (liveSeries == null) {
            return PriceIndexDatasetLoader.bundled(series);
        }

        PriceIndexDataset historical = PriceIndexDatasetLoader.bundled(series);
        PriceIndexDataset live = PriceIndexDatasetLoader.bundled(liveSeries);

        int liveStartYear = live.entries().stream()
                .mapToInt(PriceIndexEntry::year)
                .min()
                .orElseThrow(() -> new IllegalStateException(liveSeries + " dataset is empty"));

        List<PriceIndexEntry> combined = new ArrayList<>(historical.entries().stream()
                .filter(e -> e.year() < liveStartYear)
                .toList());
        combined.addAll(live.entries());

        return new PriceIndexDataset(series, combined);
    }
}
