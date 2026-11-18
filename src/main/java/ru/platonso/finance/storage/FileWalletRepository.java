package ru.platonso.finance.storage;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import ru.platonso.finance.exception.ValidationException;
import ru.platonso.finance.model.Wallet;

public final class FileWalletRepository implements WalletRepository {
  private final Path dir;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public FileWalletRepository(Path dir) {
    this.dir = dir;
  }

  public Optional<Wallet> load(String login) {
    Path file = FileSupport.userFile(dir, login);
    if (!Files.exists(file)) return Optional.empty();
    try {
      Wallet w = gson.fromJson(Files.readString(file), Wallet.class);
      if (w == null) throw new JsonParseException("пустой JSON");
      return Optional.of(w);
    } catch (Exception e) {
      throw new ValidationException(
          "Файл кошелька пользователя '" + login + "' повреждён: " + e.getMessage());
    }
  }

  public void save(String login, Wallet wallet) {
    FileSupport.atomicWrite(FileSupport.userFile(dir, login), gson.toJson(wallet));
  }
}
