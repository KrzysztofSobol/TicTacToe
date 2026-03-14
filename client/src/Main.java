import pl.edu.rsi.tictactoe.GameState;
import pl.edu.rsi.tictactoe.MoveResult;
import pl.edu.rsi.tictactoe.TicTacToeService;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

/**
 * Punkt wejścia klienta gry kółko i krzyżyk (KKlient).
 *
 * KKlient łączy się z serwerem przez RMI i wywołuje zdalne metody
 * obiektu TicTacToeService (KKSerwer). Cała logika gry jest po stronie
 * serwera; klient odpowiada wyłącznie za:
 * - interfejs konsolowy (wczytywanie ruchów gracza, wyświetlanie planszy)
 * - wysyłanie ruchów do serwera i prezentację wyników
 * - polling – co sekundę sprawdza stan gry gdy czeka na ruch przeciwnika
 *
 * Walidacja po stronie klienta:
 * - sprawdza poprawność formatu wejścia (liczba całkowita 0–2)
 * Ostateczna walidacja zawsze odbywa się na serwerze.
 */
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

    /**
     * Czeka, aż gra przejdzie ze stanu WAITING_FOR_PLAYER do IN_PROGRESS.
     * Wyświetla komunikat co sekundę.
     */
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

    /**
     * Główna pętla gry. W każdej iteracji:
     * 1. Pobiera aktualny stan gry z serwera.
     * 2. Wyświetla planszę.
     * 3. Jeśli gra zakończona – pokazuje wynik i pyta o reset.
     * 4. Jeśli jest kolej gracza – wczytuje ruch i wysyła do serwera.
     * 5. Jeśli czeka na ruch przeciwnika – polling co sekundę.
     */
    private static void playGame(TicTacToeService game, String playerId, char mySymbol,
                                  Scanner scanner)
            throws RemoteException, InterruptedException {

        boolean keepPlaying = true;

        while (keepPlaying) {
            GameState state = game.getGameState(playerId);
            printHeader(mySymbol);
            System.out.println(state.renderBoard());

            GameState.Status status = state.getStatus();

            if (status == GameState.Status.WIN) {
                if (state.getWinnerSymbol() == mySymbol) {
                    System.out.println(">>> WYGRYWASZ! Gratulacje, " + state.getWinnerName() + "! <<<");
                } else {
                    System.out.println(">>> PRZEGRYWASZ. " + state.getWinnerName()
                            + " (" + state.getWinnerSymbol() + ") wygrywa. <<<");
                }
                keepPlaying = askPlayAgain(game, scanner);
                continue;
            }

            if (status == GameState.Status.DRAW) {
                System.out.println(">>> REMIS! Nikt nie wygrał. <<<");
                keepPlaying = askPlayAgain(game, scanner);
                continue;
            }

            if (game.isMyTurn(playerId)) {
                int[] move = readMove(scanner);
                int row = move[0];
                int col = move[1];

                MoveResult result = game.makeMove(playerId, row, col);
                if (!result.isValid()) {
                    System.out.println("[!] Nieprawidłowy ruch: " + result.getMessage());
                    pause(600);
                }
            } else {
                System.out.println("Czekam na ruch przeciwnika (" +
                        (mySymbol == 'X' ? 'O' : 'X') + ")...");
                Thread.sleep(POLL_INTERVAL_MS);
            }
        }
    }

    /**
     * Wczytuje ruch gracza z konsoli.
     * Walidacja po stronie klienta: sprawdza czy wejście to liczba całkowita 0–2.
     * Ostateczna walidacja (zajętość pola, kolejność) odbywa się na serwerze.
     *
     * @return int[2] z {row, col}
     */
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

    /**
     * Po zakończeniu gry pyta gracza o reset.
     * Jeśli gracz odpowie 'T', wywołuje resetGame() na serwerze.
     * Jeśli odpowie 'N', kończy grę.
     *
     * Uwaga: reset działa poprawnie gdy obaj gracze nadal są połączeni.
     *
     * @return true jeśli gra ma być kontynuowana, false jeśli należy wyjść
     */
    private static boolean askPlayAgain(TicTacToeService game, Scanner scanner)
            throws RemoteException {
        System.out.print("\nZagrać ponownie? [T/N]: ");
        String answer = scanner.nextLine().trim().toLowerCase();
        if (answer.equals("t") || answer.equals("tak") || answer.equals("y")) {
            try {
                game.resetGame();
                System.out.println("[Klient] Gra zresetowana. Nowa partia!");
                return true;
            } catch (RemoteException e) {
                System.out.println("[Info] Reset: " + e.getMessage());
                return true;
            }
        }
        return false;
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
