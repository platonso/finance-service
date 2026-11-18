package ru.platonso.finance.storage;

import java.util.*;
import ru.platonso.finance.model.Wallet;

public interface WalletRepository {
  Optional<Wallet> load(String login);

  void save(String login, Wallet wallet);
}
