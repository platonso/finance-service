package ru.platonso.finance;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import ru.platonso.finance.exception.*;
import ru.platonso.finance.model.*;
import ru.platonso.finance.service.*;

class WalletTest {
  private Wallet wallet;

  @BeforeEach
  void init() {
    wallet = new Wallet();
    wallet.addCategory(TransactionType.INCOME, "Зарплата");
    wallet.addCategory(TransactionType.EXPENSE, "Еда");
  }

  @Test
  void calculatesBalance() {
    wallet.addTransaction(
        TransactionType.INCOME, "зарплата", new BigDecimal("100"), LocalDate.now());
    wallet.addTransaction(TransactionType.EXPENSE, "еда", new BigDecimal("30"), LocalDate.now());
    assertEquals(new BigDecimal("70"), wallet.balance());
  }

  @Test
  void rejectsUnknownCategory() {
    assertThrows(
        NotFoundException.class,
        () ->
            wallet.addTransaction(
                TransactionType.EXPENSE, "Такси", BigDecimal.ONE, LocalDate.now()));
  }

  @Test
  void rejectsNegativeAmount() {
    assertThrows(
        ValidationException.class,
        () ->
            wallet.addTransaction(
                TransactionType.INCOME, "Зарплата", BigDecimal.ZERO, LocalDate.now()));
  }

  @Test
  void renamesOperationsAndBudget() {
    wallet.setBudget("Еда", new BigDecimal("50"));
    wallet.addTransaction(TransactionType.EXPENSE, "еда", new BigDecimal("10"), LocalDate.now());
    wallet.renameCategory(TransactionType.EXPENSE, "еда", "Продукты");
    assertTrue(wallet.budgets().containsKey("продукты"));
    assertEquals("продукты", wallet.transactions().getFirst().category());
  }

  @Test
  void reportFiltersCategory() {
    wallet.addTransaction(
        TransactionType.INCOME, "Зарплата", new BigDecimal("100"), LocalDate.now());
    assertEquals(
        new BigDecimal("100"), new ReportService().create(wallet, List.of("ЗАРПЛАТА")).income());
  }

  @Test
  void warningAtEightyPercent() {
    wallet.setBudget("Еда", new BigDecimal("100"));
    wallet.addTransaction(TransactionType.EXPENSE, "Еда", new BigDecimal("80"), LocalDate.now());
    assertTrue(
        new AlertService()
            .alerts(wallet, new ReportService().create(wallet, List.of())).stream()
                .anyMatch(x -> x.contains("80%")));
  }
}
