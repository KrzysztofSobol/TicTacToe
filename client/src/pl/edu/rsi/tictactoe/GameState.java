package pl.edu.rsi.tictactoe;

import java.io.Serializable;

public class GameState implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        WAITING_FOR_PLAYER,
        IN_PROGRESS,
        WIN,
        DRAW
    }

    private final char[][] board;
    private final Status status;
    private final char currentPlayerSymbol;
    private final String winnerName;
    private final char winnerSymbol;
    private final int playerCount;

    public GameState(char[][] board, Status status, char currentPlayerSymbol,
                     String winnerName, char winnerSymbol, int playerCount) {
        this.board = board;
        this.status = status;
        this.currentPlayerSymbol = currentPlayerSymbol;
        this.winnerName = winnerName;
        this.winnerSymbol = winnerSymbol;
        this.playerCount = playerCount;
    }

    public char[][] getBoard() {
        return board;
    }

    public Status getStatus() {
        return status;
    }

    public char getCurrentPlayerSymbol() {
        return currentPlayerSymbol;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public char getWinnerSymbol() {
        return winnerSymbol;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public String renderBoard() {
        String separator = "  +---+---+---+";
        StringBuilder sb = new StringBuilder();
        sb.append("    0   1   2  \n");
        sb.append(separator).append("\n");
        for (int r = 0; r < 3; r++) {
            sb.append(r).append(" |");
            for (int c = 0; c < 3; c++) {
                char cell = board[r][c];
                sb.append(" ").append(cell == ' ' ? ' ' : cell).append(" |");
            }
            sb.append("\n").append(separator).append("\n");
        }
        return sb.toString();
    }
}
