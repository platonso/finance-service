package ru.platonso.finance.storage;

import com.google.gson.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import ru.platonso.finance.exception.ValidationException;
import ru.platonso.finance.model.Wallet;

public final class FileWalletRepository implements WalletRepository {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private final Path dir;
  private final Gson gson = createGson();

  public FileWalletRepository(Path dir) {
    this.dir = dir;
  }

  private Gson createGson() {
    return new GsonBuilder()
        .registerTypeAdapter(
            LocalDate.class,
            (JsonSerializer<LocalDate>)
                (date, type, context) -> new JsonPrimitive(DATE_FORMAT.format(date)))
        .registerTypeAdapter(
            LocalDate.class,
            (JsonDeserializer<LocalDate>)
                (json, type, context) -> LocalDate.parse(json.getAsString(), DATE_FORMAT))
        .setPrettyPrinting()
        .create();
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
