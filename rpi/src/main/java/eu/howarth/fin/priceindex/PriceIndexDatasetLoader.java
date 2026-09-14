package eu.howarth.fin.priceindex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads a {@link PriceIndexDataset} from ONS's "Download full time series
 * as CSV" export — the same row shape for every MM23-dataset series
 * (RPI CHAW, CPI D7BT, ...), so one parser serves them all. This is the
 * series-agnostic generalisation of {@code RpiDatasetLoader} (kanban #981);
 * that class is left untouched for fin-optics-api, which only ever wants
 * RPI.
 */
public class PriceIndexDatasetLoader {

    private static final Map<String, Integer> MONTH_MAP = Map.ofEntries(
            Map.entry("JAN", 1),  Map.entry("FEB", 2),  Map.entry("MAR", 3),
            Map.entry("APR", 4),  Map.entry("MAY", 5),  Map.entry("JUN", 6),
            Map.entry("JUL", 7),  Map.entry("AUG", 8),  Map.entry("SEP", 9),
            Map.entry("OCT", 10), Map.entry("NOV", 11), Map.entry("DEC", 12)
    );

    public static PriceIndexDataset fromCsv(InputStream in, IndexSeries series) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<PriceIndexEntry> entries = reader.lines()
                    .map(PriceIndexDatasetLoader::parseMonthlyRow)
                    .flatMap(Optional::stream)
                    .toList();
            return new PriceIndexDataset(series, entries);
        }
    }

    public static PriceIndexDataset bundled(IndexSeries series) {
        try (InputStream in = PriceIndexDatasetLoader.class.getResourceAsStream(series.bundledResourcePath())) {
            if (in == null) {
                throw new IllegalStateException("Bundled CSV not found on classpath: " + series.bundledResourcePath());
            }
            return fromCsv(in, series);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Optional<PriceIndexEntry> parseMonthlyRow(String line) {
        String stripped = line.replace("\"", "").trim();
        String[] parts = stripped.split(",", 2);
        if (parts.length < 2) return Optional.empty();
        String[] dateTokens = parts[0].trim().split(" ");
        if (dateTokens.length != 2) return Optional.empty();
        Integer month = MONTH_MAP.get(dateTokens[1].toUpperCase());
        if (month == null) return Optional.empty();
        try {
            int year = Integer.parseInt(dateTokens[0]);
            BigDecimal index = new BigDecimal(parts[1].trim());
            return Optional.of(new PriceIndexEntry(year, month, index));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
