package eu.howarth.fin.planning;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class BankAccountTest {

    private static final BankAccount SAVINGS = new BankAccount("savings", "ISA savings account", new BigDecimal("50000"));
    private static final BankAccount EMPTY   = new BankAccount("current", "current account", BigDecimal.ZERO);

    @Test
    void startingLiquidity_returnsStartBalance() {
        assertEquals(0, new BigDecimal("50000").compareTo(SAVINGS.startingLiquidity()));
    }

    @Test
    void startingLiquidity_zeroBalance_returnsZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(EMPTY.startingLiquidity()));
    }

    @Test
    void positions_alwaysEmpty() {
        assertTrue(SAVINGS.positions(YearMonth.of(2024, 1), YearMonth.of(2030, 1)).isEmpty());
    }

    @Test
    void flows_alwaysEmpty() {
        assertTrue(SAVINGS.flows(YearMonth.of(2024, 1), YearMonth.of(2030, 1)).isEmpty());
    }
}
