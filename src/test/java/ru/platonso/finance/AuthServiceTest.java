package ru.platonso.finance;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import ru.platonso.finance.exception.AuthException;
import ru.platonso.finance.exception.ValidationException;
import ru.platonso.finance.model.User;
import ru.platonso.finance.service.AuthService;
import ru.platonso.finance.storage.UserRepository;

class AuthServiceTest {
  @Test
  void registersAndLogsInUser() {
    AuthService service = new AuthService(new MemoryUsers());
    User registered = service.register("иван", "pass");
    assertEquals("иван", service.login("иван", "pass").login());
    assertNotEquals("pass", registered.passwordHash());
  }

  @Test
  void rejectsDuplicateLogin() {
    AuthService service = new AuthService(new MemoryUsers());
    service.register("user1", "pass");
    assertThrows(ValidationException.class, () -> service.register("user1", "pass"));
  }

  @Test
  void rejectsWrongPassword() {
    AuthService service = new AuthService(new MemoryUsers());
    service.register("user1", "pass");
    assertThrows(AuthException.class, () -> service.login("user1", "wrong"));
  }

  @Test
  void rejectsShortLogin() {
    assertThrows(
        ValidationException.class, () -> new AuthService(new MemoryUsers()).register("ab", "pass"));
  }

  @Test
  void rejectsShortPassword() {
    assertThrows(
        ValidationException.class,
        () -> new AuthService(new MemoryUsers()).register("user1", "123"));
  }

  static final class MemoryUsers implements UserRepository {
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
}
