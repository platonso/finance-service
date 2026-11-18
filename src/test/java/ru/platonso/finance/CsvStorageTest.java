package ru.platonso.finance;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.platonso.finance.exception.ValidationException;
import ru.platonso.finance.model.*;
import ru.platonso.finance.service.CsvService;
import ru.platonso.finance.storage.*;

class CsvStorageTest {
  @TempDir Path directory;

  @Test
  void csvRoundTripPreservesOperations() {
    Wallet source = walletWithExpense();
    source.addTransaction(
        TransactionType.EXPENSE, "Еда", new BigDecimal("99.50"), LocalDate.of(2026, 1, 1));
    StringWriter output = new StringWriter();
    new CsvService().export(source, output);
    Wallet target = walletWithExpense();
    new CsvService().importInto(target, new StringReader(output.toString()));
    assertEquals(source.transactions(), target.transactions());
  }

  @Test
  void invalidCsvDoesNotChangeWallet() {
    Wallet wallet = walletWithExpense();
    assertThrows(
        ValidationException.class,
        () ->
            new CsvService()
                .importInto(
                    wallet, new StringReader("type,category,amount,date\nEXPENSE,еда,no,date")));
    assertTrue(wallet.transactions().isEmpty());
  }

  @Test
  void walletRepositoryPersistsWallet() {
    Wallet wallet = walletWithExpense();
    wallet.addTransaction(TransactionType.EXPENSE, "Еда", BigDecimal.TEN, LocalDate.now());
    FileWalletRepository repository = new FileWalletRepository(directory);
    repository.save("Иван", wallet);
    assertEquals(BigDecimal.TEN.negate(), repository.load("Иван").orElseThrow().balance());
  }

  @Test
  void userRepositoryPersistsUser() {
    FileUserRepository repository = new FileUserRepository(directory);
    User user = new User("Иван", "salt", "hash");
    repository.save(user);
    assertEquals(user, new FileUserRepository(directory).find("Иван").orElseThrow());
  }

  @Test
  void corruptedUsersFileHasClearError() throws IOException {
    Files.writeString(directory.resolve("users.json"), "not json");
    assertThrows(ValidationException.class, () -> new FileUserRepository(directory));
  }

  private Wallet walletWithExpense() {
    Wallet wallet = new Wallet();
    wallet.addCategory(TransactionType.EXPENSE, "Еда");
    return wallet;
  }
}
