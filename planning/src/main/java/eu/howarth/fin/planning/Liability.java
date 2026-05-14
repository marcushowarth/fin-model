package eu.howarth.fin.planning;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.YearMonth;
import java.util.NavigableMap;
import java.util.TreeMap;

public record Liability(
        String name,
        String description,
        YearMonth start,
        BigDecimal balance,
        BigDecimal annualInterestRate,
        BigDecimal monthlyRepayment
) implements FinancialItem {

    @Override
    public NavigableMap<YearMonth, BigDecimal> positions(YearMonth from, YearMonth to) {
        NavigableMap<YearMonth, BigDecimal> result = new TreeMap<>();
        iterate(from, to, (month, bal, payment) -> result.put(month, bal.negate()));
        return result;
    }

    @Override
    public NavigableMap<YearMonth, BigDecimal> flows(YearMonth from, YearMonth to) {
        NavigableMap<YearMonth, BigDecimal> result = new TreeMap<>();
        iterate(from, to, (month, bal, payment) -> result.put(month, payment.negate()));
        return result;
    }

    private void iterate(YearMonth from, YearMonth to, MonthConsumer consumer) {
        if (start.isAfter(to)) return;

        BigDecimal factor = BigDecimal.valueOf(Math.pow(1 + annualInterestRate.doubleValue(), 1.0 / 12));
        BigDecimal bal = balance;
        YearMonth current = start;

        while (!current.isAfter(to) && bal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal balWithInterest = bal.multiply(factor, MathContext.DECIMAL64);
            BigDecimal payment = monthlyRepayment.min(balWithInterest);

            if (!current.isBefore(from)) {
                consumer.accept(current, bal, payment);
            }

            bal = balWithInterest.subtract(payment).max(BigDecimal.ZERO);
            current = current.plusMonths(1);
        }
    }

    @FunctionalInterface
    private interface MonthConsumer {
        void accept(YearMonth month, BigDecimal balance, BigDecimal payment);
    }
}
