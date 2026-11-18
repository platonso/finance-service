package ru.platonso.finance.service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.*;
import ru.platonso.finance.exception.NotFoundException;
import ru.platonso.finance.model.*;

public final class ReportService {
  public record Report(
      BigDecimal income,
      BigDecimal expense,
      Map<String, BigDecimal> incomes,
      Map<String, BigDecimal> expenses,
      List<BudgetStatus> budgets) {}

  public Report create(Wallet w, Collection<String> requested) {
    Set<String> cats = normalizeAndValidate(w, requested);
    Map<String, BigDecimal> inc = sums(w, TransactionType.INCOME, cats);
    Map<String, BigDecimal> exp = sums(w, TransactionType.EXPENSE, cats);
    List<BudgetStatus> budgets =
        w.budgets().entrySet().stream()
            .map(
                e -> {
                  BigDecimal spent =
                      sums(w, TransactionType.EXPENSE, Set.of(e.getKey()))
                          .getOrDefault(e.getKey(), BigDecimal.ZERO);
                  return new BudgetStatus(
                      e.getKey(), e.getValue(), spent, e.getValue().subtract(spent));
                })
            .toList();
    return new Report(total(inc), total(exp), inc, exp, budgets);
  }

  public Map<String, BigDecimal> sums(Wallet w, TransactionType type, Set<String> categories) {
    return w.transactions().stream()
        .filter(t -> t.type() == type)
        .filter(t -> categories.isEmpty() || categories.contains(t.category()))
        .collect(
            Collectors.groupingBy(
                Transaction::category,
                LinkedHashMap::new,
                Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)));
  }

  private Set<String> normalizeAndValidate(Wallet w, Collection<String> requested) {
    if (requested == null || requested.isEmpty()) return Set.of();
    Set<String> known =
        Stream.concat(
                w.categories(TransactionType.INCOME).stream(),
                w.categories(TransactionType.EXPENSE).stream())
            .collect(Collectors.toSet());
    Set<String> answer = new LinkedHashSet<>();
    for (String c : requested) {
      String key = Wallet.key(c);
      if (!known.contains(key)) throw new NotFoundException("Категория не найдена: " + c);
      answer.add(key);
    }
    return answer;
  }

  private BigDecimal total(Map<String, BigDecimal> values) {
    return values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
