package eu.howarth.fin.planning;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.NavigableMap;

public sealed interface FinancialItem
        permits Asset, BankAccount, Expenditure, Income, Investment, Liability {
    String name();
    String description();

    default NavigableMap<YearMonth, BigDecimal> positions(YearMonth from, YearMonth to) {
        return Collections.emptyNavigableMap();
    }

    default NavigableMap<YearMonth, BigDecimal> flows(YearMonth from, YearMonth to) {
        return Collections.emptyNavigableMap();
    }

    default BigDecimal startingLiquidity() {
        return BigDecimal.ZERO;
    }
}
