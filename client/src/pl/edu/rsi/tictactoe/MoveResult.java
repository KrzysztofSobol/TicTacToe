package pl.edu.rsi.tictactoe;

import java.io.Serializable;

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
