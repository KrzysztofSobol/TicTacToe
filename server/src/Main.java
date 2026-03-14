import pl.edu.rsi.tictactoe.TicTacToeServiceImpl;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("=== Gra Kółko i Krzyżyk ===");
            System.out.print("Podaj hostname/IP serwera [localhost]: ");
            String host = scanner.nextLine().trim();
            if (host.isEmpty()) {
                host = "localhost";
            }

            System.setProperty("java.rmi.server.hostname", host);

            LocateRegistry.createRegistry(1099);
            System.out.println("[Serwer] RMI Registry uruchomione na porcie 1099.");

            TicTacToeServiceImpl gameService = new TicTacToeServiceImpl();
            String bindUrl = "//" + host + "/TicTacToe";
            Naming.rebind(bindUrl, gameService);

            System.out.println("[Serwer] Serwer gry zarejestrowany pod: " + bindUrl);
            System.out.println("[Serwer] Oczekiwanie na graczy...");
            System.out.println("[Serwer] Wciśnij Enter, aby zatrzymać serwer.");

            scanner.nextLine();
            System.out.println("[Serwer] Zatrzymywanie...");

        } catch (RemoteException e) {
            System.err.println("[Błąd] Nie można uruchomić serwera RMI: " + e.getMessage());
            e.printStackTrace();
        } catch (MalformedURLException e) {
            System.err.println("[Błąd] Nieprawidłowy URL RMI: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
