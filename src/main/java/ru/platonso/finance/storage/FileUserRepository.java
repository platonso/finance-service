package ru.platonso.finance.storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import ru.platonso.finance.exception.ValidationException;
import ru.platonso.finance.model.User;

public final class FileUserRepository implements UserRepository {
  private final Path file;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private final Map<String, User> users = new LinkedHashMap<>();

  public FileUserRepository(Path dataDir) {
    file = dataDir.resolve("users.json");
    load();
  }

  private void load() {
    if (!Files.exists(file)) return;
    try {
      Type type = new TypeToken<List<User>>() {}.getType();
      List<User> list = gson.fromJson(Files.readString(file), type);
      if (list != null) list.forEach(u -> users.put(u.login(), u));
    } catch (Exception e) {
      throw new ValidationException("Файл пользователей повреждён: " + e.getMessage());
    }
  }

  public Optional<User> find(String login) {
    return Optional.ofNullable(users.get(login));
  }

  public Collection<User> findAll() {
    return List.copyOf(users.values());
  }

  public void save(User user) {
    users.put(user.login(), user);
    FileSupport.atomicWrite(file, gson.toJson(users.values()));
  }
}
