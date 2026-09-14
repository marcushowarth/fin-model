package eu.howarth.fin.priceindex;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

/**
 * Real-terms adjustment driven by whichever {@link PriceIndexDataset} is
 * passed in — series-agnostic generalisation of {@code RpiAdjuster}
 * (kanban #981). The same value adjusted by RPI vs CPI over the same
 * period will differ, since the two indexes weight and cover spending
 * differently (RPI includes mortgage interest/housing costs that CPI
 * excludes, among other basket differences) — that divergence is the
 * whole point of offering both.
 */
public class PriceIndexAdjuster {

    public static BigDecimal adjust(BigDecimal value, int fromYear, int toYear, PriceIndexDataset dataset) {
        if (fromYear == toYear) return value;
        BigDecimal fromIndex = dataset.indexForYear(fromYear);
        BigDecimal toIndex   = dataset.indexForYear(toYear);
        return value.multiply(toIndex.divide(fromIndex, 10, RoundingMode.HALF_UP));
    }

    public static BigDecimal adjust(BigDecimal value, YearMonth from, YearMonth to, PriceIndexDataset dataset) {
        if (from.equals(to)) return value;
        BigDecimal fromIndex = dataset.indexForMonth(from.getYear(), from.getMonthValue());
        BigDecimal toIndex   = dataset.indexForMonth(to.getYear(), to.getMonthValue());
        return value.multiply(toIndex.divide(fromIndex, 10, RoundingMode.HALF_UP));
    }
}
