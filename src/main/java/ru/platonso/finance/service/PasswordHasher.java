package ru.platonso.finance.service;

import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import ru.platonso.finance.exception.ValidationException;

final class PasswordHasher {
  private PasswordHasher() {}

  static String salt() {
    byte[] b = new byte[16];
    new SecureRandom().nextBytes(b);
    return Base64.getEncoder().encodeToString(b);
  }

  static String hash(String password, String salt) {
    try {
      PBEKeySpec spec =
          new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), 120_000, 256);
      byte[] result =
          SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
      spec.clearPassword();
      return Base64.getEncoder().encodeToString(result);
    } catch (Exception e) {
      throw new ValidationException("Не удалось обработать пароль.");
    }
  }

  static boolean matches(String password, String salt, String expected) {
    return MessageDigest.isEqual(
        Base64.getDecoder().decode(hash(password, salt)), Base64.getDecoder().decode(expected));
  }
}
