package ru.platonso.finance;

import java.io.*;
import java.nio.file.Path;
import ru.platonso.finance.cli.ConsoleApp;
import ru.platonso.finance.service.*;
import ru.platonso.finance.storage.*;

public final class Main {
  public static void main(String[] args) {
    Path data = Path.of("data");
    UserRepository users = new FileUserRepository(data);
    WalletRepository wallets = new FileWalletRepository(data);
    new ConsoleApp(new AuthService(users), wallets, new TransferService(users, wallets))
        .run(new InputStreamReader(System.in), new PrintWriter(System.out, true));
  }
}
