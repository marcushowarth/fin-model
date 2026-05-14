package eu.howarth.fin.planning;

import java.math.BigDecimal;

public record BankAccount(
        String name,
        String description,
        BigDecimal startBalance
) implements FinancialItem {

    @Override
    public BigDecimal startingLiquidity() {
        return startBalance;
    }
}
