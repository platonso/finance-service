package ru.platonso.finance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import ru.platonso.finance.model.*;

public final class WalletService {
  public void addCategory(Wallet w, TransactionType type, String name) {
    w.addCategory(type, name);
  }

  public void renameCategory(Wallet w, TransactionType type, String oldName, String newName) {
    w.renameCategory(type, oldName, newName);
  }

  public void add(Wallet w, TransactionType type, String category, BigDecimal amount) {
    w.addTransaction(type, category, amount, LocalDate.now());
  }

  public void setBudget(Wallet w, String category, BigDecimal amount) {
    w.setBudget(category, amount);
  }

  public void removeBudget(Wallet w, String category) {
    w.removeBudget(category);
  }
}
