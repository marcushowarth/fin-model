package eu.howarth.fin.priceindex.projection;

import eu.howarth.fin.priceindex.PriceIndexEntry;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public record ConstantIndexProjection(BigDecimal annualRate) implements IndexProjection {

    @Override
    public List<PriceIndexEntry> project(PriceIndexEntry anchor, YearMonth upTo) {
        YearMonth anchorMonth = YearMonth.of(anchor.year(), anchor.month());
        if (!upTo.isAfter(anchorMonth)) return List.of();

        double monthlyFactor = Math.pow(1 + annualRate.doubleValue(), 1.0 / 12);

        List<PriceIndexEntry> entries = new ArrayList<>();
        YearMonth current = anchorMonth.plusMonths(1);
        BigDecimal currentIndex = anchor.index();

        while (!current.isAfter(upTo)) {
            currentIndex = currentIndex.multiply(BigDecimal.valueOf(monthlyFactor), MathContext.DECIMAL64);
            entries.add(new PriceIndexEntry(current.getYear(), current.getMonthValue(), currentIndex));
            current = current.plusMonths(1);
        }

        return List.copyOf(entries);
    }
}
