package ru.platonso.finance;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.platonso.finance.exception.NotFoundException;
import ru.platonso.finance.model.*;
import ru.platonso.finance.service.*;

class ReportAlertTest {
  private Wallet wallet;
  private final ReportService reports = new ReportService();

  @BeforeEach
  void setUp() {
    wallet = new Wallet();
    wallet.addCategory(TransactionType.INCOME, "Зарплата");
    wallet.addCategory(TransactionType.EXPENSE, "Еда");
    wallet.addCategory(TransactionType.EXPENSE, "Такси");
  }

  @Test
  void calculatesIncomeAndExpenseTotals() {
    add(TransactionType.INCOME, "Зарплата", "1000");
    add(TransactionType.EXPENSE, "Еда", "200");
    ReportService.Report report = reports.create(wallet, List.of());
    assertEquals(new BigDecimal("1000"), report.income());
    assertEquals(new BigDecimal("200"), report.expense());
  }

  @Test
  void calculatesOnlySelectedCategories() {
    add(TransactionType.EXPENSE, "Еда", "200");
    add(TransactionType.EXPENSE, "Такси", "300");
    ReportService.Report report = reports.create(wallet, List.of("еда"));
    assertEquals(new BigDecimal("200"), report.expense());
    assertFalse(report.expenses().containsKey("такси"));
  }

  @Test
  void rejectsMissingSelectedCategory() {
    assertThrows(NotFoundException.class, () -> reports.create(wallet, List.of("Отпуск")));
  }

  @Test
  void warnsWhenBudgetExceeded() {
    wallet.setBudget("Еда", new BigDecimal("100"));
    add(TransactionType.EXPENSE, "Еда", "101");
    assertTrue(
        new AlertService()
            .alerts(wallet, reports.create(wallet, List.of())).stream()
                .anyMatch(x -> x.contains("Превышен")));
  }

  @Test
  void warnsWhenBalanceIsNegative() {
    add(TransactionType.EXPENSE, "Такси", "1");
    assertTrue(
        new AlertService()
            .alerts(wallet, reports.create(wallet, List.of())).stream()
                .anyMatch(x -> x.contains("Баланс")));
  }

  private void add(TransactionType type, String category, String amount) {
    wallet.addTransaction(type, category, new BigDecimal(amount), LocalDate.now());
  }
}
