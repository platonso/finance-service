package ru.platonso.finance.cli;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import ru.platonso.finance.exception.*;
import ru.platonso.finance.model.*;
import ru.platonso.finance.service.*;
import ru.platonso.finance.storage.WalletRepository;

public final class ConsoleApp {
  private final AuthService auth;
  private final WalletRepository walletRepo;
  private final WalletService walletService = new WalletService();
  private final ReportService reports = new ReportService();
  private final AlertService alerts = new AlertService();
  private final TransferService transfers;
  private final CsvService csv = new CsvService();
  private final Map<String, Wallet> loaded = new LinkedHashMap<>();
  private String current;

  public ConsoleApp(AuthService auth, WalletRepository walletRepo, TransferService transfers) {
    this.auth = auth;
    this.walletRepo = walletRepo;
    this.transfers = transfers;
  }

  public void run(Reader input, PrintWriter out) {
    try (BufferedReader in = new BufferedReader(input)) {
      printWelcome(out);
      String line;
      while ((line = in.readLine()) != null) {
        try {
          if (execute(Tokenizer.split(line), out)) break;
        } catch (AppException e) {
          out.println("Ошибка: " + e.getMessage());
        } catch (Exception e) {
          out.println("Ошибка: непредвиденная проблема: " + e.getMessage());
        }
      }
    } catch (IOException e) {
      out.println("Ошибка чтения: " + e.getMessage());
    } finally {
      saveAll(out);
    }
  }

  private void printWelcome(PrintWriter out) {
    out.println("Finance service. Введите help для справки.");
  }

  private boolean execute(List<String> a, PrintWriter out) {
    if (a.isEmpty()) return false;
    String cmd = a.getFirst().toLowerCase(Locale.ROOT);
    if (cmd.equals("exit")) {
      return true;
    }
    if (cmd.equals("help")) {
      help(out);
      return false;
    }
    if (cmd.equals("register")) {
      need(a, 3);
      auth.register(a.get(1), a.get(2));
      out.println("Пользователь зарегистрирован.");
      return false;
    }
    if (cmd.equals("login")) {
      need(a, 3);
      current = auth.login(a.get(1), a.get(2)).login();
      loaded.computeIfAbsent(current, l -> walletRepo.load(l).orElseGet(Wallet::new));
      out.println("Вход выполнен: " + current);
      return false;
    }
    if (cmd.equals("logout")) {
      current = null;
      out.println("Вы вышли.");
      return false;
    }
    switch (cmd) {
      case "category" -> category(a, currentWallet(), out);
      case "income", "expense" -> operation(a, currentWallet(), cmd, out);
      case "budget" -> budget(a, currentWallet(), out);
      case "stats" -> stats(a, currentWallet(), out);
      case "report-file" -> reportFile(a, currentWallet(), out);
      case "transfer" -> transfer(a, currentWallet(), out);
      case "export" -> export(a, currentWallet(), out);
      case "import" -> importCsv(a, currentWallet(), out);
      default -> throw new ValidationException("Неизвестная команда.");
    }
    return false;
  }

  private void category(List<String> a, Wallet w, PrintWriter o) {
    need(a, 2);
    if (a.get(1).equals("list")) {
      o.println("Доходы: " + w.categories(TransactionType.INCOME));
      o.println("Расходы: " + w.categories(TransactionType.EXPENSE));
      return;
    }
    need(a, 4);
    TransactionType t = type(a.get(2));
    if (a.get(1).equals("add")) walletService.addCategory(w, t, a.get(3));
    else if (a.get(1).equals("rename")) {
      need(a, 5);
      walletService.renameCategory(w, t, a.get(3), a.get(4));
    } else throw new ValidationException("category: add, rename или list.");
    o.println("Категория сохранена.");
  }

  private void operation(List<String> a, Wallet w, String cmd, PrintWriter o) {
    exact(a, 3);
    walletService.add(
        w,
        cmd.equals("income") ? TransactionType.INCOME : TransactionType.EXPENSE,
        a.get(1),
        money(a.get(2)));
    o.println("Операция добавлена.");
    showAlerts(w, o);
  }

  private void budget(List<String> a, Wallet w, PrintWriter o) {
    need(a, 2);
    switch (a.get(1)) {
      case "set", "edit" -> {
        need(a, 4);
        walletService.setBudget(w, a.get(2), money(a.get(3)));
        o.println("Бюджет сохранён.");
      }
      case "remove" -> {
        need(a, 3);
        walletService.removeBudget(w, a.get(2));
        o.println("Бюджет удалён.");
      }
      case "list" -> print(reports.create(w, List.of()), o);
      default -> throw new ValidationException("budget: set, edit, remove или list.");
    }
  }

  private void stats(List<String> a, Wallet w, PrintWriter o) {
    print(reports.create(w, a.subList(1, a.size())), o);
    showAlerts(w, o);
  }

  private void reportFile(List<String> a, Wallet w, PrintWriter o) {
    need(a, 2);
    try (PrintWriter file = new PrintWriter(Files.newBufferedWriter(Path.of(a.get(1))))) {
      print(reports.create(w, List.of()), file);
      o.println("Отчёт записан в файл.");
    } catch (IOException e) {
      throw new ValidationException("Не удалось записать отчёт: " + e.getMessage());
    }
  }

  private void transfer(List<String> a, Wallet w, PrintWriter o) {
    exact(a, 3);
    transfers.transfer(current, a.get(1), w, loaded, money(a.get(2)));
    o.println("Перевод выполнен.");
    showAlerts(w, o);
  }

  private void export(List<String> a, Wallet w, PrintWriter o) {
    need(a, 2);
    try (Writer wr = Files.newBufferedWriter(Path.of(a.get(1)))) {
      csv.export(w, wr);
      o.println("CSV экспортирован.");
    } catch (IOException e) {
      throw new ValidationException("Ошибка экспорта: " + e.getMessage());
    }
  }

  private void importCsv(List<String> a, Wallet w, PrintWriter o) {
    need(a, 2);
    try (Reader r = Files.newBufferedReader(Path.of(a.get(1)))) {
      csv.importInto(w, r);
      o.println("CSV импортирован.");
    } catch (IOException e) {
      throw new ValidationException("Ошибка импорта: " + e.getMessage());
    }
  }

  private void print(ReportService.Report r, PrintWriter o) {
    o.println("Общий доход: " + r.income().toPlainString());
    o.println("Доходы по категориям:");
    r.incomes().forEach((k, v) -> o.println(label(k) + ": " + v.toPlainString()));
    o.println("Общие расходы: " + r.expense().toPlainString());
    o.println("Расходы по категориям:");
    r.expenses().forEach((k, v) -> o.println(label(k) + ": " + v.toPlainString()));
    o.println("Бюджет по категориям:");
    for (BudgetStatus b : r.budgets())
      o.println(
          label(b.category())
              + ": "
              + b.limit().toPlainString()
              + ", Оставшийся бюджет: "
              + b.remaining().toPlainString());
  }

  private void showAlerts(Wallet w, PrintWriter o) {
    for (String s : alerts.alerts(w, reports.create(w, List.of())))
      o.println("Предупреждение: " + s);
  }

  private void saveAll(PrintWriter o) {
    loaded.forEach(walletRepo::save);
  }

  private void requireUser() {
    if (current == null) throw new AuthException("Сначала выполните login.");
  }

  private Wallet currentWallet() {
    requireUser();
    return loaded.get(current);
  }

  private void need(List<String> a, int n) {
    if (a.size() < n) throw new ValidationException("Недостаточно аргументов. Введите help.");
  }

  private void exact(List<String> a, int n) {
    if (a.size() != n) throw new ValidationException("Неверное число аргументов. Введите help.");
  }

  private TransactionType type(String s) {
    return switch (s) {
      case "income" -> TransactionType.INCOME;
      case "expense" -> TransactionType.EXPENSE;
      default -> throw new ValidationException("Тип: income или expense.");
    };
  }

  private BigDecimal money(String s) {
    try {
      return new BigDecimal(s);
    } catch (NumberFormatException e) {
      throw new ValidationException("Сумма должна быть числом.");
    }
  }

  private String label(String value) {
    return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }

  private void help(PrintWriter o) {
    o.println(
        """
            register <логин> <пароль>                                — зарегистрировать пользователя
            login <логин> <пароль>                                   — войти в аккаунт
            logout                                                   — выйти из текущего аккаунта

            category add <income|expense> <название>                 — создать категорию доходов или расходов
            category rename <income|expense> <старое> <новое>        — переименовать категорию и её операции
            category list                                            — показать все категории

            income <категория> <сумма>                               — добавить доход
            expense <категория> <сумма>                              — добавить расход
            budget set|edit <категория> <сумма>                      — установить или изменить бюджет
            budget remove <категория>                                — удалить бюджет категории
            budget list                                              — показать бюджеты и остатки

            stats [категория1 категория2 ...]                        — показать статистику по выбранным категориям
            report-file <путь>                                       — сохранить отчёт в текстовый файл
            export <файл.csv>                                        — выгрузить операции в CSV
            import <файл.csv>                                        — загрузить операции из CSV

            transfer <логин> <сумма>                                 — перевести деньги пользователю
            help                                                     — вывести эту справку
            exit                                                     — сохранить данные и завершить программу

            Названия с пробелами заключайте в двойные кавычки.
            """);
  }
}
