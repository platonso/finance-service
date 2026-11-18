package ru.platonso.finance.service;

import java.util.regex.Pattern;
import ru.platonso.finance.exception.*;
import ru.platonso.finance.model.User;
import ru.platonso.finance.storage.UserRepository;

public final class AuthService {
  private static final Pattern LOGIN = Pattern.compile("[\\p{L}0-9_-]{3,30}");
  private final UserRepository users;

  public AuthService(UserRepository users) {
    this.users = users;
  }

  public User register(String login, String password) {
    valid(login, password);
    if (users.find(login).isPresent())
      throw new ValidationException("Пользователь уже существует.");
    String salt = PasswordHasher.salt();
    User user = new User(login, salt, PasswordHasher.hash(password, salt));
    users.save(user);
    return user;
  }

  public User login(String login, String password) {
    valid(login, password);
    User user =
        users.find(login).orElseThrow(() -> new AuthException("Неверный логин или пароль."));
    if (!PasswordHasher.matches(password, user.salt(), user.passwordHash()))
      throw new AuthException("Неверный логин или пароль.");
    return user;
  }

  private void valid(String login, String password) {
    if (login == null || !LOGIN.matcher(login).matches())
      throw new ValidationException("Логин: 3–30 букв, цифр, '_' или '-'.");
    if (password == null || password.length() < 4)
      throw new ValidationException("Пароль должен содержать не менее 4 символов.");
  }
}
