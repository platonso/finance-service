package ru.platonso.finance;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.platonso.finance.exception.*;
import ru.platonso.finance.model.*;
import ru.platonso.finance.service.TransferService;
import ru.platonso.finance.storage.*;

class TransferServiceTest {
  private final Users users = new Users();
  private final Wallets wallets = new Wallets();
  private TransferService service;
  private Wallet sender;
  private Map<String, Wallet> loaded;

  @BeforeEach
  void setUp() {
    users.save(new User("sender", "salt", "hash"));
    users.save(new User("recipient", "salt", "hash"));
    sender = new Wallet();
    sender.addCategory(TransactionType.INCOME, "Зарплата");
    sender.addTransaction(
        TransactionType.INCOME, "Зарплата", new BigDecimal("100"), LocalDate.now());
    loaded = new HashMap<>();
    loaded.put("sender", sender);
    service = new TransferService(users, wallets);
  }

  @Test
  void transfersMoneyBetweenWallets() {
    Wallet recipient =
        service.transfer("sender", "recipient", sender, loaded, new BigDecimal("40"));
    assertEquals(new BigDecimal("60"), sender.balance());
    assertEquals(new BigDecimal("40"), recipient.balance());
  }

  @Test
  void rejectsTransferWithInsufficientFunds() {
    assertThrows(
        ValidationException.class,
        () -> service.transfer("sender", "recipient", sender, loaded, new BigDecimal("101")));
  }

  @Test
  void rejectsTransferToSelf() {
    assertThrows(
        ValidationException.class,
        () -> service.transfer("sender", "sender", sender, loaded, BigDecimal.ONE));
  }

  @Test
  void rejectsTransferToUnknownUser() {
    assertThrows(
        NotFoundException.class,
        () -> service.transfer("sender", "missing", sender, loaded, BigDecimal.ONE));
  }

  private static final class Users implements UserRepository {
    private final Map<String, User> values = new HashMap<>();

    public Optional<User> find(String login) {
      return Optional.ofNullable(values.get(login));
    }

    public void save(User user) {
      values.put(user.login(), user);
    }

    public Collection<User> findAll() {
      return values.values();
    }
  }

  private static final class Wallets implements WalletRepository {
    private final Map<String, Wallet> values = new HashMap<>();

    public Optional<Wallet> load(String login) {
      return Optional.ofNullable(values.get(login));
    }

    public void save(String login, Wallet wallet) {
      values.put(login, wallet);
    }
  }
}
