package ru.platonso.finance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import ru.platonso.finance.exception.*;
import ru.platonso.finance.model.*;
import ru.platonso.finance.storage.*;

public final class TransferService {
  private final UserRepository users;
  private final WalletRepository wallets;

  public TransferService(UserRepository users, WalletRepository wallets) {
    this.users = users;
    this.wallets = wallets;
  }

  public Wallet transfer(
      String from, String to, Wallet sender, Map<String, Wallet> loaded, BigDecimal amount) {
    if (from.equals(to)) throw new ValidationException("Нельзя переводить средства самому себе.");
    if (amount == null || amount.signum() <= 0)
      throw new ValidationException("Сумма должна быть положительной.");
    if (sender.balance().compareTo(amount) < 0)
      throw new ValidationException("Недостаточно средств для перевода.");
    if (users.find(to).isEmpty()) throw new NotFoundException("Получатель не найден: " + to);
    Wallet recipient = loaded.computeIfAbsent(to, l -> wallets.load(l).orElseGet(Wallet::new));
    String out = "Перевод: " + to, in = "Перевод от: " + from;
    sender.addCategoryIfMissing(TransactionType.EXPENSE, out);
    recipient.addCategoryIfMissing(TransactionType.INCOME, in);
    sender.addTransaction(TransactionType.EXPENSE, out, amount, LocalDate.now());
    recipient.addTransaction(TransactionType.INCOME, in, amount, LocalDate.now());
    return recipient;
  }
}
