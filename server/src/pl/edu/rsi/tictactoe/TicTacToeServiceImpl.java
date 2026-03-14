package pl.edu.rsi.tictactoe;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TicTacToeServiceImpl extends UnicastRemoteObject implements TicTacToeService {

    private static final long serialVersionUID = 1L;

    private final char[][] board = new char[3][3];
    private final Map<String, Player> players = new LinkedHashMap<>();
    private String currentPlayerId = null;
    private GameState.Status status = GameState.Status.WAITING_FOR_PLAYER;
    private String winnerPlayerId = null;

    private static class Player {
        final String id;
        final String name;
        final char symbol;

        Player(String id, String name, char symbol) {
            this.id = id;
            this.name = name;
            this.symbol = symbol;
        }
    }

    public TicTacToeServiceImpl() throws RemoteException {
        super();
        clearBoard();
    }

    private void clearBoard() {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                board[r][c] = ' ';
    }

    @Override
    public synchronized String joinGame(String playerName) throws RemoteException {
        if (players.size() >= 2) {
            throw new RemoteException("Gra jest pełna. Maksymalnie 2 graczy.");
        }

        String playerId = UUID.randomUUID().toString();
        char symbol = players.isEmpty() ? 'X' : 'O';
        Player player = new Player(playerId, playerName, symbol);
        players.put(playerId, player);

        System.out.println("[Serwer] " + playerName + " dołączył jako '" + symbol + "' (ID: " + playerId.substring(0, 8) + "...)");

        if (players.size() == 2) {
            status = GameState.Status.IN_PROGRESS;
            currentPlayerId = players.keySet().iterator().next();

            System.out.println("[Serwer] Gra rozpoczęta! Ruch gracza X ("
                    + players.get(currentPlayerId).name + ")");
        } else {
            System.out.println("[Serwer] Oczekiwanie na drugiego gracza...");
        }

        return playerId + ":" + symbol;
    }

    @Override
    public synchronized MoveResult makeMove(String playerId, int row, int col)
            throws RemoteException {
        Player player = players.get(playerId);
        if (player == null) {
            return new MoveResult(false, "Nieznany gracz.", buildGameState(null));
        }

        if (status == GameState.Status.WAITING_FOR_PLAYER) {
            return new MoveResult(false, "Oczekiwanie na drugiego gracza.", buildGameState(player));
        }
        if (status == GameState.Status.WIN || status == GameState.Status.DRAW) {
            return new MoveResult(false, "Gra już się zakończyła. Zresetuj grę, aby zagrać ponownie.",
                    buildGameState(player));
        }
        if (!playerId.equals(currentPlayerId)) {
            return new MoveResult(false, "Nie twoja kolej!", buildGameState(player));
        }
        if (row < 0 || row > 2 || col < 0 || col > 2) {
            return new MoveResult(false, "Nieprawidłowa pozycja. Wiersz i kolumna muszą być z zakresu 0–2.",
                    buildGameState(player));
        }
        if (board[row][col] != ' ') {
            return new MoveResult(false, "Pole (" + row + "," + col + ") jest już zajęte.",
                    buildGameState(player));
        }

        board[row][col] = player.symbol;

        if (checkWin(player.symbol)) {
            status = GameState.Status.WIN;
            winnerPlayerId = playerId;
            System.out.println("[Serwer] " + player.name + " (" + player.symbol + ") wygrywa!");
            printServerBoard();
            return new MoveResult(true, player.name + " wygrywa!", buildGameState(player));
        }

        if (checkDraw()) {
            status = GameState.Status.DRAW;
            System.out.println("[Serwer] Remis!");
            printServerBoard();
            return new MoveResult(true, "Remis!", buildGameState(player));
        }

        for (Player p : players.values()) {
            if (!p.id.equals(playerId)) {
                currentPlayerId = p.id;
                break;
            }
        }

        Player next = players.get(currentPlayerId);
        System.out.println("[Serwer] " + player.name + " zagrał (" + row + "," + col + "). "
                + "Ruch gracza " + next.name + " (" + next.symbol + ")");
        printServerBoard();

        return new MoveResult(true, "Ruch zaakceptowany. Teraz kolej: " + next.name
                + " (" + next.symbol + ")", buildGameState(player));
    }

    @Override
    public synchronized GameState getGameState(String playerId) throws RemoteException {
        Player player = players.get(playerId);
        return buildGameState(player);
    }

    @Override
    public synchronized boolean isMyTurn(String playerId) throws RemoteException {
        return status == GameState.Status.IN_PROGRESS && playerId.equals(currentPlayerId);
    }

    @Override
    public synchronized void resetGame() throws RemoteException {
        if (status == GameState.Status.IN_PROGRESS) {
            throw new RemoteException("Nie można zresetować gry w toku.");
        }
        clearBoard();
        winnerPlayerId = null;

        if (players.size() == 2) {
            status = GameState.Status.IN_PROGRESS;
            for (Player p : players.values()) {
                if (p.symbol == 'X') {
                    currentPlayerId = p.id;
                    break;
                }
            }
            System.out.println("[Serwer] Gra zresetowana. Ruch gracza X ("
                    + players.get(currentPlayerId).name + ")");
        } else {
            status = GameState.Status.WAITING_FOR_PLAYER;
            currentPlayerId = null;
            System.out.println("[Serwer] Gra zresetowana. Oczekiwanie na graczy.");
        }
    }

    private boolean checkWin(char sym) {
        for (int r = 0; r < 3; r++) {
            if (board[r][0] == sym && board[r][1] == sym && board[r][2] == sym) return true;
        }
        for (int c = 0; c < 3; c++) {
            if (board[0][c] == sym && board[1][c] == sym && board[2][c] == sym) return true;
        }
        if (board[0][0] == sym && board[1][1] == sym && board[2][2] == sym) return true;
        if (board[0][2] == sym && board[1][1] == sym && board[2][0] == sym) return true;
        return false;
    }

    private boolean checkDraw() {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                if (board[r][c] == ' ') return false;
        return true;
    }

    private GameState buildGameState(Player requestingPlayer) {
        char[][] boardCopy = new char[3][3];
        for (int r = 0; r < 3; r++)
            boardCopy[r] = board[r].clone();

        char currentSym = ' ';
        if (currentPlayerId != null) {
            Player cp = players.get(currentPlayerId);
            if (cp != null) currentSym = cp.symbol;
        }

        String winnerName = null;
        char winnerSym = ' ';
        if (winnerPlayerId != null) {
            Player wp = players.get(winnerPlayerId);
            if (wp != null) {
                winnerName = wp.name;
                winnerSym = wp.symbol;
            }
        }

        return new GameState(boardCopy, status, currentSym, winnerName, winnerSym, players.size());
    }

    private void printServerBoard() {
        System.out.println("  0 1 2");
        for (int r = 0; r < 3; r++) {
            System.out.print(r + " ");
            for (int c = 0; c < 3; c++) {
                System.out.print(board[r][c] == ' ' ? "." : board[r][c]);
                if (c < 2) System.out.print("|");
            }
            System.out.println();
        }
    }
}
