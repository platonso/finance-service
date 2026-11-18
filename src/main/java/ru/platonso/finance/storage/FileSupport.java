package ru.platonso.finance.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import ru.platonso.finance.exception.ValidationException;

final class FileSupport {
  private FileSupport() {}

  static Path userFile(Path dir, String login) {
    return dir.resolve("wallet-" + sha256(login) + ".json");
  }

  static String sha256(String text) {
    try {
      byte[] bytes =
          MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder();
      for (byte b : bytes) out.append(String.format("%02x", b));
      return out.toString();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  static void atomicWrite(Path file, String value) {
    try {
      Files.createDirectories(file.getParent());
      Path temp = Files.createTempFile(file.getParent(), "finance-", ".tmp");
      Files.writeString(temp, value, StandardCharsets.UTF_8);
      try {
        Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new ValidationException("Не удалось сохранить файл: " + e.getMessage());
    }
  }
}
