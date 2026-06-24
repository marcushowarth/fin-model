package eu.howarth.fin.planning;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FinancialModelTest {

    private static final YearMonth JAN = YearMonth.of(2024, 1);
    private static final YearMonth FEB = YearMonth.of(2024, 2);
    private static final YearMonth MAR = YearMonth.of(2024, 3);

    // --- Coverage of every month in range ---

    @Test
    void project_allMonthsPresentInResults() {
        var proj = new FinancialModel(List.of()).project(JAN, MAR);
        assertEquals(3, proj.netWorth().size());
        assertTrue(proj.netWorth().containsKey(JAN));
        assertTrue(proj.netWorth().containsKey(FEB));
        assertTrue(proj.netWorth().containsKey(MAR));
    }

    // --- Empty model ---

    @Test
    void project_emptyModel_zeroNetWorthAndCash() {
        var proj = new FinancialModel(List.of()).project(JAN, JAN);
        assertAmount("0", proj.netWorth().get(JAN));
        assertAmount("0", proj.cashPosition().get(JAN));
    }

    @Test
    void project_emptyModel_noWarnings() {
        assertTrue(new FinancialModel(List.of()).project(JAN, MAR).warnings().isEmpty());
    }

    // --- Cash position ---

    @Test
    void project_bankAccountOnly_cashEqualsStartBalance() {
        var proj = new FinancialModel(List.of(
                new BankAccount("savings", "", bd("10000"))
        )).project(JAN, JAN);
        assertAmount("10000", proj.cashPosition().get(JAN));
    }

    @Test
    void project_multipleBankAccounts_liquiditiesSummed() {
        var proj = new FinancialModel(List.of(
                new BankAccount("current", "", bd("1000")),
                new BankAccount("savings", "", bd("9000"))
        )).project(JAN, JAN);
        assertAmount("10000", proj.cashPosition().get(JAN));
    }

    @Test
    void project_income_accumulatesCashEachMonth() {
        var proj = new FinancialModel(List.of(
                new Income("salary", "", JAN, Optional.empty(), bd("1000"), BigDecimal.ZERO)
        )).project(JAN, MAR);
        assertAmount("1000", proj.cashPosition().get(JAN));
        assertAmount("2000", proj.cashPosition().get(FEB));
        assertAmount("3000", proj.cashPosition().get(MAR));
    }

    @Test
    void project_expenditure_reducesCashEachMonth() {
        var proj = new FinancialModel(List.of(
                new BankAccount("bank", "", bd("5000")),
                new Expenditure("bills", "", JAN, Optional.of(MAR), bd("1000"), BigDecimal.ZERO)
        )).project(JAN, MAR);
        assertAmount("4000", proj.cashPosition().get(JAN));
        assertAmount("3000", proj.cashPosition().get(FEB));
        assertAmount("2000", proj.cashPosition().get(MAR));
    }

    @Test
    void project_bankAccountAndIncome_startBalancePlusCumulativeFlows() {
        var proj = new FinancialModel(List.of(
                new BankAccount("bank", "", bd("5000")),
                new Income("salary", "", JAN, Optional.empty(), bd("1000"), BigDecimal.ZERO)
        )).project(JAN, FEB);
        assertAmount("6000", proj.cashPosition().get(JAN));
        assertAmount("7000", proj.cashPosition().get(FEB));
    }

    // --- Net worth ---

    @Test
    void project_assetAddsToNetWorthNotCash() {
        var proj = new FinancialModel(List.of(
                new Asset("flat", "", JAN, bd("200000"), BigDecimal.ZERO, Optional.empty()),
                new BankAccount("savings", "", bd("10000"))
        )).project(JAN, JAN);
        assertAmount("10000",  proj.cashPosition().get(JAN));
        assertAmount("210000", proj.netWorth().get(JAN));
    }

    @Test
    void project_liabilityReducesNetWorth() {
        var proj = new FinancialModel(List.of(
                new Asset("house", "", JAN, bd("200000"), BigDecimal.ZERO, Optional.empty()),
                new Liability("mortgage", "", JAN, bd("150000"), BigDecimal.ZERO, bd("1000")),
                new BankAccount("bank", "", bd("5000"))
        )).project(JAN, JAN);
        // cashPosition = 5000 - 1000 (repayment) = 4000
        // item positions: house=200000, mortgage=-150000 → 50000
        // netWorth = 50000 + 4000 = 54000
        assertAmount("4000",  proj.cashPosition().get(JAN));
        assertAmount("54000", proj.netWorth().get(JAN));
    }

    @Test
    void project_assetSale_proceedsFlowToCash_netWorthUnchanged() {
        // Asset sold in Feb — net worth should be the same before and after (converted, not created)
        var flat = new Asset("flat", "", JAN, bd("100000"), BigDecimal.ZERO, Optional.of(FEB));
        var proj = new FinancialModel(List.of(flat)).project(JAN, FEB);
        // Jan: position=100000, cash=0, netWorth=100000
        // Feb: position=0 (sold), cash=100000 (proceeds), netWorth=100000
        assertAmount("0",      proj.cashPosition().get(JAN));
        assertAmount("100000", proj.cashPosition().get(FEB));
        assertAmount("100000", proj.netWorth().get(JAN));
        assertAmount("100000", proj.netWorth().get(FEB));
    }

    // --- Solvency warnings ---

    @Test
    void project_solvencyWarning_whenCashGoesNegative() {
        var proj = new FinancialModel(List.of(
                new BankAccount("bank", "", bd("500")),
                new Expenditure("car", "", JAN, Optional.empty(), bd("1000"), BigDecimal.ZERO)
        )).project(JAN, JAN);
        assertEquals(1, proj.warnings().size());
        assertEquals(JAN, proj.warnings().get(0).month());
        assertAmount("-500", proj.warnings().get(0).cashPosition());
    }

    @Test
    void project_noWarning_whenCashPositiveThroughout() {
        var proj = new FinancialModel(List.of(
                new BankAccount("bank", "", bd("5000")),
                new Expenditure("bills", "", JAN, Optional.of(MAR), bd("1000"), BigDecimal.ZERO)
        )).project(JAN, MAR);
        assertTrue(proj.warnings().isEmpty());
    }

    @Test
    void project_warningGeneratedForEveryNegativeMonth() {
        var proj = new FinancialModel(List.of(
                new Expenditure("bills", "", JAN, Optional.of(MAR), bd("1000"), BigDecimal.ZERO)
        )).project(JAN, MAR);
        assertEquals(3, proj.warnings().size());
    }

    // --- Item positions ---

    @Test
    void project_itemPositions_trackedByName() {
        var flat = new Asset("flat", "family home", JAN, bd("200000"), BigDecimal.ZERO, Optional.empty());
        var proj = new FinancialModel(List.of(flat)).project(JAN, JAN);
        assertTrue(proj.itemPositions().containsKey("flat"));
        assertAmount("200000", proj.itemPositions().get("flat").get(JAN));
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Expected " + expected + " but was " + actual);
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
