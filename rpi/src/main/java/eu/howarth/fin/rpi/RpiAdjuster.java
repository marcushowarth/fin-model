package eu.howarth.fin.rpi;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RpiAdjuster {

    /**
     *
     * @param value
     * @param fromYear
     * @param toYear
     * @param dataset
     * @return value × (rpi[referenceYear] / rpi[valueYear])
     */
    public static BigDecimal adjust(BigDecimal value, int fromYear, int toYear, RpiDataset dataset) {
        if (fromYear == toYear) return value;

        BigDecimal fromIndex = dataset.indexForYear(fromYear);
        BigDecimal toIndex   = dataset.indexForYear(toYear);

        return value.multiply(toIndex.divide(fromIndex, 10, RoundingMode.HALF_UP));
    }

}
