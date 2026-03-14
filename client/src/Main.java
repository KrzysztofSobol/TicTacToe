import pl.edu.rsi.tictactoe.GameState;
import pl.edu.rsi.tictactoe.MoveResult;
import pl.edu.rsi.tictactoe.TicTacToeService;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class Main {

    private static final int POLL_INTERVAL_MS = 1000;

    public static void  main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== KKlient – Gra Kółko i Krzyżyk (RMI) ===");

        System.out.print("Podaj hostname/IP serwera [localhost]: ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) host = "localhost";

        System.out.print("Podaj swoją nazwę: ");
        String playerName = scanner.nextLine().trim();
        if (playerName.isEmpty()) playerName = "Gracz";

        TicTacToeService game;
        try {
            String lookupUrl = "//" + host + "/TicTacToe";
            System.out.println("[Klient] Łączę się z serwerem: " + lookupUrl);
            game = (TicTacToeService) Naming.lookup(lookupUrl);
            System.out.println("[Klient] Połączono z serwerem.");
        } catch (NotBoundException e) {
            System.err.println("[Błąd] Serwer gry nie jest dostępny pod nazwą 'TicTacToe'.");
            return;
        } catch (MalformedURLException e) {
            System.err.println("[Błąd] Nieprawidłowy URL: " + e.getMessage());
            return;
        } catch (RemoteException e) {
            System.err.println("[Błąd] Nie można połączyć się z serwerem: " + e.getMessage());
            return;
        }

        try {
            String joinResponse = game.joinGame(playerName);
            String[] parts = joinResponse.split(":", 2);
            String playerId = parts[0];
            char mySymbol = parts[1].charAt(0);

            System.out.println("[Klient] Dołączono do gry jako '" + mySymbol + "'.");
            if (mySymbol == 'X') {
                System.out.println("[Klient] Jesteś graczem X – zaczynasz pierwszy!");
            } else {
                System.out.println("[Klient] Jesteś graczem O – przeciwnik (X) zaczyna.");
            }

            waitForGameStart(game, playerId);
            playGame(game, playerId, mySymbol, scanner);

        } catch (RemoteException e) {
            System.err.println("[Błąd] Błąd komunikacji z serwerem: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[Błąd] Przerwano oczekiwanie.");
        }

        System.out.println("\nDziękuję za grę! Wciśnij Enter, aby wyjść.");
        scanner.nextLine();
    }

    private static void waitForGameStart(TicTacToeService game, String playerId)
            throws RemoteException, InterruptedException {
        System.out.println("[Klient] Oczekiwanie na drugiego gracza...");
        GameState state;
        boolean dotPrinted = false;
        while (true) {
            state = game.getGameState(playerId);
            if (state.getStatus() != GameState.Status.WAITING_FOR_PLAYER) break;
            if (!dotPrinted) {
                System.out.print("         ");
                dotPrinted = true;
            }
            System.out.print(".");
            System.out.flush();
            Thread.sleep(POLL_INTERVAL_MS);
        }
        System.out.println();
        System.out.println("[Klient] Gra rozpoczęta! Plansza 3x3, pozycje: wiersz i kolumna 0–2.");
    }

    private static void playGame(TicTacToeService game, String playerId, char mySymbol, Scanner scanner)
            throws RemoteException, InterruptedException {
        boolean waitingPrinted = false;

        while (true) {
            GameState state = game.getGameState(playerId);
            GameState.Status status = state.getStatus();

            if (status == GameState.Status.WIN) {
                printHeader(mySymbol);
                System.out.println(state.renderBoard());
                if (state.getWinnerSymbol() == mySymbol) {
                    System.out.println(">>> WYGRYWASZ! Gratulacje, " + state.getWinnerName() + "! <<<");
                } else {
                    System.out.println(">>> PRZEGRYWASZ. " + state.getWinnerName()
                            + " (" + state.getWinnerSymbol() + ") wygrywa. <<<");
                }
                return;
            }

            if (status == GameState.Status.DRAW) {
                printHeader(mySymbol);
                System.out.println(state.renderBoard());
                System.out.println(">>> REMIS! Nikt nie wygrał. <<<");
                return;
            }

            if (game.isMyTurn(playerId)) {
                waitingPrinted = false;
                printHeader(mySymbol);
                System.out.println(state.renderBoard());
                int[] move = readMove(scanner);
                MoveResult result = game.makeMove(playerId, move[0], move[1]);
                if (!result.isValid()) {
                    System.out.println("[!] Nieprawidłowy ruch: " + result.getMessage());
                    pause(600);
                }
            } else {
                if (!waitingPrinted) {
                    printHeader(mySymbol);
                    System.out.println(state.renderBoard());
                    System.out.println("Czekam na ruch przeciwnika ("
                            + (mySymbol == 'X' ? 'O' : 'X') + ")...");
                    waitingPrinted = true;
                }
                Thread.sleep(POLL_INTERVAL_MS);
            }
        }
    }

    private static int[] readMove(Scanner scanner) {
        int row = -1, col = -1;
        boolean valid = false;
        while (!valid) {
            try {
                System.out.print("Twój ruch – wiersz (0-2): ");
                row = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("Twój ruch – kolumna (0-2): ");
                col = Integer.parseInt(scanner.nextLine().trim());
                if (row >= 0 && row <= 2 && col >= 0 && col <= 2) {
                    valid = true;
                } else {
                    System.out.println("[!] Wartości muszą być w zakresie 0–2.");
                }
            } catch (NumberFormatException e) {
                System.out.println("[!] Podaj liczbę całkowitą (0, 1 lub 2).");
            }
        }
        return new int[]{row, col};
    }

    private static void printHeader(char mySymbol) {
        System.out.println("\n=============================");
        System.out.println("  Kółko i Krzyżyk  |  Ty: '" + mySymbol + "'");
        System.out.println("=============================");
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
