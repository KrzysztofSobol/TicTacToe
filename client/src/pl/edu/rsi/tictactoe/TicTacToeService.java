package pl.edu.rsi.tictactoe;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TicTacToeService extends Remote {

    String joinGame(String playerName) throws RemoteException;
    MoveResult makeMove(String playerId, int row, int col) throws RemoteException;
    GameState getGameState(String playerId) throws RemoteException;
    boolean isMyTurn(String playerId) throws RemoteException;
    void resetGame() throws RemoteException;

    void leaveGame(String playerId) throws RemoteException;
}
