package ru.platonso.finance.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import ru.platonso.finance.exception.*;

public final class Wallet {
  private Set<String> incomeCategories = new LinkedHashSet<>();
  private Set<String> expenseCategories = new LinkedHashSet<>();
  private Map<String, BigDecimal> budgets = new LinkedHashMap<>();
  private List<Transaction> transactions = new ArrayList<>();

  public static String key(String value) {
    if (value == null || value.trim().isEmpty())
      throw new ValidationException("Название не должно быть пустым.");
    return value.trim().toLowerCase(Locale.ROOT);
  }

  private Set<String> mutableCategories(TransactionType type) {
    return type == TransactionType.INCOME ? incomeCategories : expenseCategories;
  }

  public void addCategory(TransactionType type, String name) {
    Set<String> set = mutableCategories(type);
    String n = key(name);
    if (!set.add(n)) throw new ValidationException("Категория уже существует: " + name.trim());
  }

  public void addCategoryIfMissing(TransactionType type, String name) {
    mutableCategories(type).add(key(name));
  }

  public void renameCategory(TransactionType type, String oldName, String newName) {
    String old = key(oldName), next = key(newName);
    Set<String> set = mutableCategories(type);
    if (!set.contains(old)) throw new NotFoundException("Категория не найдена: " + oldName);
    if (!old.equals(next) && set.contains(next))
      throw new ValidationException("Категория уже существует: " + newName);
    set.remove(old);
    set.add(next);
    if (type == TransactionType.EXPENSE && budgets.containsKey(old)) {
      BigDecimal b = budgets.remove(old);
      budgets.put(next, b);
    }
    for (int i = 0; i < transactions.size(); i++) {
      Transaction t = transactions.get(i);
      if (t.type() == type && t.category().equals(old))
        transactions.set(i, new Transaction(t.type(), next, t.amount(), t.date()));
    }
  }

  public void addTransaction(
      TransactionType type, String category, BigDecimal amount, LocalDate date) {
    if (amount == null || amount.signum() <= 0)
      throw new ValidationException("Сумма должна быть положительным числом.");
    String c = key(category);
    if (!categories(type).contains(c))
      throw new NotFoundException("Категория не найдена: " + category);
    transactions.add(new Transaction(type, c, amount, date == null ? LocalDate.now() : date));
  }

  public void setBudget(String category, BigDecimal amount) {
    if (amount == null || amount.signum() <= 0)
      throw new ValidationException("Бюджет должен быть положительным числом.");
    String c = key(category);
    if (!expenseCategories.contains(c))
      throw new NotFoundException("Категория расходов не найдена: " + category);
    budgets.put(c, amount);
  }

  public void removeBudget(String category) {
    if (budgets.remove(key(category)) == null)
      throw new NotFoundException("Бюджет не найден: " + category);
  }

  public Set<String> categories(TransactionType type) {
    return Collections.unmodifiableSet(mutableCategories(type));
  }

  public Map<String, BigDecimal> budgets() {
    return Collections.unmodifiableMap(budgets);
  }

  public List<Transaction> transactions() {
    return Collections.unmodifiableList(transactions);
  }

  public BigDecimal balance() {
    return transactions.stream()
        .map(t -> t.type() == TransactionType.INCOME ? t.amount() : t.amount().negate())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
