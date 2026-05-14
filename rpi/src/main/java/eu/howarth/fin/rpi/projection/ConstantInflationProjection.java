package eu.howarth.fin.rpi.projection;

import eu.howarth.fin.rpi.RpiEntry;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public record ConstantInflationProjection(BigDecimal annualRate) implements InflationProjection {

    @Override
    public List<RpiEntry> project(RpiEntry anchor, YearMonth upTo) {
        YearMonth anchorMonth = YearMonth.of(anchor.year(), anchor.month());
        if (!upTo.isAfter(anchorMonth)) return List.of();

        double monthlyFactor = Math.pow(1 + annualRate.doubleValue(), 1.0 / 12);

        List<RpiEntry> entries = new ArrayList<>();
        YearMonth current = anchorMonth.plusMonths(1);
        BigDecimal currentIndex = anchor.index();

        while (!current.isAfter(upTo)) {
            currentIndex = currentIndex.multiply(BigDecimal.valueOf(monthlyFactor), MathContext.DECIMAL64);
            entries.add(new RpiEntry(current.getYear(), current.getMonthValue(), currentIndex));
            current = current.plusMonths(1);
        }

        return List.copyOf(entries);
    }
}
