package ru.platonso.finance.service;

import java.math.BigDecimal;
import java.util.*;
import ru.platonso.finance.model.*;

public final class AlertService {
  public List<String> alerts(Wallet w, ReportService.Report report) {
    Set<String> result = new LinkedHashSet<>();
    for (BudgetStatus b : report.budgets()) {
      if (b.spent().compareTo(b.limit()) > 0) result.add("Превышен бюджет: " + b.category());
      else if (b.spent()
              .multiply(BigDecimal.valueOf(100))
              .compareTo(b.limit().multiply(BigDecimal.valueOf(80)))
          >= 0) result.add("Использовано 80% бюджета: " + b.category());
    }
    if (report.expense().compareTo(report.income()) > 0) result.add("Расходы превысили доходы.");
    if (w.balance().signum() <= 0) result.add("Баланс нулевой или отрицательный.");
    return List.copyOf(result);
  }
}
