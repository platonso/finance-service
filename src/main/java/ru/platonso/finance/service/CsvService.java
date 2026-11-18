package ru.platonso.finance.service;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import ru.platonso.finance.exception.ValidationException;
import ru.platonso.finance.model.*;

public final class CsvService {
  public void export(Wallet w, Writer writer) {
    try {
      writer.write("type,category,amount,date\n");
      for (Transaction t : w.transactions())
        writer.write(
            row(t.type().name(), t.category(), t.amount().toPlainString(), t.date().toString())
                + "\n");
    } catch (IOException e) {
      throw new ValidationException("Ошибка экспорта CSV: " + e.getMessage());
    }
  }

  public void importInto(Wallet w, Reader reader) {
    List<Transaction> parsed = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(reader)) {
      String header = br.readLine();
      if (!"type,category,amount,date".equals(header))
        throw new ValidationException("Неверный заголовок CSV.");
      String line;
      int no = 1;
      while ((line = br.readLine()) != null) {
        no++;
        List<String> p = parse(line);
        if (p.size() != 4) throw new ValidationException("Некорректная строка CSV: " + no);
        TransactionType type = TransactionType.valueOf(p.get(0));
        BigDecimal amount = new BigDecimal(p.get(2));
        LocalDate date = LocalDate.parse(p.get(3));
        if (amount.signum() <= 0 || !w.categories(type).contains(Wallet.key(p.get(1))))
          throw new ValidationException("Некорректные данные CSV: строка " + no);
        parsed.add(new Transaction(type, Wallet.key(p.get(1)), amount, date));
      }
    } catch (ValidationException e) {
      throw e;
    } catch (Exception e) {
      throw new ValidationException("Некорректный CSV: " + e.getMessage());
    }
    for (Transaction t : parsed) w.addTransaction(t.type(), t.category(), t.amount(), t.date());
  }

  private String row(String... values) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      if (i > 0) b.append(',');
      b.append('"').append(values[i].replace("\"", "\"\"")).append('"');
    }
    return b.toString();
  }

  private List<String> parse(String line) {
    List<String> out = new ArrayList<>();
    StringBuilder b = new StringBuilder();
    boolean quotes = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        if (quotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          b.append(c);
          i++;
        } else quotes = !quotes;
      } else if (c == ',' && !quotes) {
        out.add(b.toString());
        b.setLength(0);
      } else b.append(c);
    }
    if (quotes) throw new ValidationException("Незакрытая кавычка в CSV.");
    out.add(b.toString());
    return out;
  }
}
