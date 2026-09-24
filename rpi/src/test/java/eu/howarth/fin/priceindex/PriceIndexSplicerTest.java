package eu.howarth.fin.priceindex;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriceIndexSplicerTest {

    @Test
    void seriesWithNoSpliceTarget_returnsItsOwnBundledDataset() {
        PriceIndexDataset spliced = PriceIndexSplicer.spliceToPresent(IndexSeries.RPI);
        PriceIndexDataset plain = PriceIndexDatasetLoader.bundled(IndexSeries.RPI);
        assertEquals(plain.entries().size(), spliced.entries().size());
        assertEquals(IndexSeries.RPI, spliced.series());
    }

    @Test
    void millenniumSeries_splicesOntoLiveSeries() {
        PriceIndexDataset spliced = PriceIndexSplicer.spliceToPresent(IndexSeries.RPI_MILLENNIUM);
        // Reaches back to the Millennium start...
        assertDoesNotThrow(() -> spliced.indexForYear(1209));
        // ...and forward past where the Millennium bundle itself ends (2016) —
        // proves the live RPI data was actually appended, not just re-served.
        PriceIndexDataset liveRpi = PriceIndexDatasetLoader.bundled(IndexSeries.RPI);
        int latestLiveYear = liveRpi.entries().stream().mapToInt(PriceIndexEntry::year).max().orElseThrow();
        assertTrue(latestLiveYear > 2016, "test assumes live RPI data extends past 2016");
        assertDoesNotThrow(() -> spliced.indexForYear(latestLiveYear));
    }

    @Test
    void millenniumSeries_resultTaggedWithRequestedSeries() {
        PriceIndexDataset spliced = PriceIndexSplicer.spliceToPresent(IndexSeries.RPI_MILLENNIUM);
        assertEquals(IndexSeries.RPI_MILLENNIUM, spliced.series());
    }

    @Test
    void overlapYears_preferLiveDataOverMillenniumSplice() {
        // Both datasets cover e.g. 2000 — the live ONS figure should win,
        // not the frozen Millennium-spreadsheet figure, for any year both have.
        PriceIndexDataset spliced = PriceIndexSplicer.spliceToPresent(IndexSeries.RPI_MILLENNIUM);
        PriceIndexDataset liveRpi = PriceIndexDatasetLoader.bundled(IndexSeries.RPI);
        assertEquals(0, liveRpi.indexForYear(2000).compareTo(spliced.indexForYear(2000)));
    }

    @Test
    void historicalYearsBeforeTheSpliceAppearExactlyOnce() {
        // The Millennium side is annual (one entry per year); the live ONS side is
        // monthly (up to 12 per year) — that's expected. What must NOT happen is a
        // historical year being duplicated by also existing on the live side.
        PriceIndexDataset spliced = PriceIndexSplicer.spliceToPresent(IndexSeries.CPI_MILLENNIUM);
        PriceIndexDataset live = PriceIndexDatasetLoader.bundled(IndexSeries.CPI);
        int liveStartYear = live.entries().stream().mapToInt(PriceIndexEntry::year).min().orElseThrow();

        List<Integer> historicalSideYears = spliced.entries().stream()
                .map(PriceIndexEntry::year)
                .filter(year -> year < liveStartYear)
                .toList();
        assertEquals(historicalSideYears.size(), historicalSideYears.stream().distinct().count(),
                "each pre-live year should appear exactly once, from the historical side only");
    }

    @Test
    void adjuster_worksAcrossTheSpliceBoundary() {
        PriceIndexDataset spliced = PriceIndexSplicer.spliceToPresent(IndexSeries.RPI_MILLENNIUM);
        // 1930 (Millennium side) -> 2016 (live side) should compute without error
        BigDecimal adjusted = PriceIndexAdjuster.adjust(BigDecimal.valueOf(300), 1930, 2016, spliced);
        assertTrue(adjusted.compareTo(BigDecimal.valueOf(300)) > 0, "a century of inflation should increase the value");
    }
}
