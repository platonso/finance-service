package ru.platonso.finance.storage;

import java.util.*;
import ru.platonso.finance.model.User;

public interface UserRepository {
  Optional<User> find(String login);

  void save(User user);

  Collection<User> findAll();
}
