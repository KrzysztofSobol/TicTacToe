package pl.edu.rsi.tictactoe;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Zdalny interfejs RMI serwera gry kółko i krzyżyk (KKSerwer).
 *
 * Implementacja po stronie serwera zarządza całą logiką gry:
 * stanem planszy, kolejnością ruchów, walidacją i wykrywaniem końca gry.
 * Klient (KKlient) wywołuje te metody zdalnie przez RMI.
 */
public interface TicTacToeService extends Remote {

    /**
     * Dołącza do gry. Pierwszy gracz otrzymuje symbol 'X', drugi 'O'.
     * Gra startuje automatycznie gdy dołączy drugi gracz.
     *
     * @param playerName nazwa gracza widoczna dla przeciwnika
     * @return token w formacie "playerId:symbol" (np. "abc123:X")
     * @throws RemoteException gdy gra jest już pełna (2 graczy)
     */
    String joinGame(String playerName) throws RemoteException;

    /**
     * Wykonuje ruch na planszy w podanej pozycji (wiersz, kolumna).
     * Serwer waliduje ruch: czy gracz ma teraz ruch, czy pole jest wolne,
     * czy pozycja jest prawidłowa. Po ruchu sprawdza wygraną/remis.
     *
     * @param playerId unikalny identyfikator gracza (z joinGame)
     * @param row      wiersz planszy (0–2)
     * @param col      kolumna planszy (0–2)
     * @return MoveResult z wynikiem walidacji i nowym stanem gry
     * @throws RemoteException błąd komunikacji RMI
     */
    MoveResult makeMove(String playerId, int row, int col) throws RemoteException;

    /**
     * Zwraca aktualny stan gry: planszę, status, czyi teraz ruch, wynik.
     *
     * @param playerId unikalny identyfikator gracza
     * @return aktualny GameState
     * @throws RemoteException błąd komunikacji RMI
     */
    GameState getGameState(String playerId) throws RemoteException;

    /**
     * Sprawdza, czy teraz jest kolej podanego gracza.
     *
     * @param playerId unikalny identyfikator gracza
     * @return true jeśli gracz może teraz wykonać ruch
     * @throws RemoteException błąd komunikacji RMI
     */
    boolean isMyTurn(String playerId) throws RemoteException;

    /**
     * Resetuje grę po jej zakończeniu (WIN lub DRAW).
     * Plansza jest czyszczona, gracze pozostają zarejestrowani,
     * X zaczyna od nowa.
     *
     * @throws RemoteException gdy gra jest jeszcze w toku
     */
    void resetGame() throws RemoteException;
}
