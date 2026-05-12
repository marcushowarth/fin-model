package eu.howarth.fin.rpi;

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

public class RpiDatasetLoader {

    private static final Map<String, Integer> MONTH_MAP = Map.ofEntries(
            Map.entry("JAN", 1),  Map.entry("FEB", 2),  Map.entry("MAR", 3),
            Map.entry("APR", 4),  Map.entry("MAY", 5),  Map.entry("JUN", 6),
            Map.entry("JUL", 7),  Map.entry("AUG", 8),  Map.entry("SEP", 9),
            Map.entry("OCT", 10), Map.entry("NOV", 11), Map.entry("DEC", 12)
    );

    public static RpiDataset fromCsv(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<RpiEntry> entries = reader.lines()
                    .map(RpiDatasetLoader::parseMonthlyRow)
                    .flatMap(Optional::stream)
                    .toList();
            return new RpiDataset(entries);
        }
    }

    public static RpiDataset bundled() {
        try (InputStream in = RpiDatasetLoader.class.getResourceAsStream("ons-chaw.csv")) {
            if (in == null) throw new IllegalStateException("Bundled ONS CHAW CSV not found on classpath");
            return fromCsv(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Optional<RpiEntry> parseMonthlyRow(String line) {
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
            return Optional.of(new RpiEntry(year, month, index));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
