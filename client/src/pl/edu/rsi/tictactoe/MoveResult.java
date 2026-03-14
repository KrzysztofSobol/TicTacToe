package pl.edu.rsi.tictactoe;

import java.io.Serializable;

/**
 * Wynik wykonania ruchu, zwracany przez makeMove().
 * Jest serializowalny – przesyłany przez RMI jako kopia (value object).
 *
 * Zawiera:
 * - valid   – czy ruch był poprawny i został zaakceptowany
 * - message – komunikat dla gracza (np. błąd walidacji lub info o stanie)
 * - gameState – aktualny stan gry po ruchu (lub bieżący przy odrzuceniu)
 */
public class MoveResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean valid;
    private final String message;
    private final GameState gameState;

    public MoveResult(boolean valid, String message, GameState gameState) {
        this.valid = valid;
        this.message = message;
        this.gameState = gameState;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public GameState getGameState() {
        return gameState;
    }
}
