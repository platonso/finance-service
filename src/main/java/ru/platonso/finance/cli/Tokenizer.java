package ru.platonso.finance.cli;

import java.util.*;
import ru.platonso.finance.exception.ValidationException;

public final class Tokenizer {
  private Tokenizer() {}

  public static List<String> split(String input) {
    List<String> r = new ArrayList<>();
    StringBuilder b = new StringBuilder();
    boolean q = false;
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (c == '"') q = !q;
      else if (Character.isWhitespace(c) && !q) {
        if (!b.isEmpty()) {
          r.add(b.toString());
          b.setLength(0);
        }
      } else b.append(c);
    }
    if (q) throw new ValidationException("Незакрытая двойная кавычка.");
    if (!b.isEmpty()) r.add(b.toString());
    return r;
  }
}
