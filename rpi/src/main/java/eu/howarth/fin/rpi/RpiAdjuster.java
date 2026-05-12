package eu.howarth.fin.rpi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

public class RpiAdjuster {

    public static BigDecimal adjust(BigDecimal value, int fromYear, int toYear, RpiDataset dataset) {
        if (fromYear == toYear) return value;
        BigDecimal fromIndex = dataset.indexForYear(fromYear);
        BigDecimal toIndex   = dataset.indexForYear(toYear);
        return value.multiply(toIndex.divide(fromIndex, 10, RoundingMode.HALF_UP));
    }

    public static BigDecimal adjust(BigDecimal value, YearMonth from, YearMonth to, RpiDataset dataset) {
        if (from.equals(to)) return value;
        BigDecimal fromIndex = dataset.indexForMonth(from.getYear(), from.getMonthValue());
        BigDecimal toIndex   = dataset.indexForMonth(to.getYear(), to.getMonthValue());
        return value.multiply(toIndex.divide(fromIndex, 10, RoundingMode.HALF_UP));
    }

}
