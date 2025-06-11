import java.io.*;
import java.net.*;

public class PlayerHandler extends Thread {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private PlayerHandler opponent;
    private final char mark;
    private String playerName = "Player";
    private boolean myTurn;
    private boolean gameEnded = false;
    private boolean wantsRestart = false;
    private boolean disconnected = false;
    private char[][] board; // Reference to shared game board

    public PlayerHandler(Socket socket, char mark, char[][] board) throws IOException {
        this.socket = socket;
        this.mark = mark;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.myTurn = (mark == 'X'); // X always starts
        this.board = board;
    }

    public void setOpponent(PlayerHandler opponent) {
        this.opponent = opponent;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public boolean wantsRestart() {
        return wantsRestart;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    public void resetGame() {
        gameEnded = false;
        wantsRestart = false;
        myTurn = (mark == 'X');
    }

    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("NAME ")) {
                    playerName = line.substring(5);
                    System.out.println(playerName + " joined as Player " + mark);
                } else if (line.startsWith("Chat ")) {
                    String chatMsg = "Chat " + playerName + ": " + line.substring(5);
                    sendMessage(chatMsg);
                    if (opponent != null) opponent.sendMessage(chatMsg);
                } else if (line.startsWith("Move ") && !gameEnded) {
                    if (myTurn && opponent != null) {
                        // Parse move
                        String[] parts = line.split(" ");
                        if (parts.length == 2) {
                            String[] coords = parts[1].split(",");
                            int row = Integer.parseInt(coords[0]);
                            int col = Integer.parseInt(coords[1]);

                            // Check if valid move (in bounds and empty cell)
                            if (row >= 0 && row < 3 && col >= 0 && col < 3 && board[row][col] == ' ') {
                                // Update board state
                                board[row][col] = mark;

                                // Broadcast move to both players
                                String moveMsg = "Move " + mark + " " + row + "," + col;
                                sendMessage(moveMsg);
                                opponent.sendMessage(moveMsg);

                                // Check for win or draw
                                char gameResult = checkGameState(row, col);
                                if (gameResult == mark) {
                                    // Win condition
                                    gameEnded = true;
                                    opponent.gameEnded = true;
                                    String winMsg = "GAME_OVER " + playerName + " wins!";
                                    sendMessage(winMsg);
                                    opponent.sendMessage(winMsg);
                                } else if (gameResult == 'D') {
                                    // Draw condition
                                    gameEnded = true;
                                    opponent.gameEnded = true;
                                    String drawMsg = "GAME_OVER Draw!";
                                    sendMessage(drawMsg);
                                    opponent.sendMessage(drawMsg);
                                } else {
                                    // Continue game, switch turns
                                    myTurn = false;
                                    opponent.myTurn = true;

                                    sendMessage("Opponent's turn");
                                    opponent.sendMessage("Your turn");
                                }
                            }
                        }
                    }
                } else if (line.equals("RESTART") && gameEnded) {
                    // Player wants to restart
                    wantsRestart = true;
                    sendMessage("Chat System: Waiting for opponent to restart...");

                    // Notify opponent
                    if (opponent != null) {
                        opponent.sendMessage("Chat System: " + playerName + " wants to restart the game.");

                        // If both players want to restart, the GameMonitorThread will handle it
                        if (opponent.wantsRestart) {
                            sendMessage("Chat System: Both players ready. Starting new game...");
                            opponent.sendMessage("Chat System: Both players ready. Starting new game...");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Connection lost for " + playerName);
            disconnected = true;
            if (opponent != null && !opponent.gameEnded) {
                opponent.sendMessage("GAME_OVER Opponent disconnected");
                opponent.gameEnded = true;
            }
        } finally {
            try {
                socket.close();
                disconnected = true;
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    private char checkGameState(int lastRow, int lastCol) {
        // Check for win

        // Check row
        if (board[lastRow][0] == mark && board[lastRow][1] == mark && board[lastRow][2] == mark) {
            return mark; // Win
        }

        // Check column
        if (board[0][lastCol] == mark && board[1][lastCol] == mark && board[2][lastCol] == mark) {
            return mark; // Win
        }

        // Check diagonals
        if (lastRow == lastCol) {
            // Main diagonal
            if (board[0][0] == mark && board[1][1] == mark && board[2][2] == mark) {
                return mark; // Win
            }
        }

        if (lastRow + lastCol == 2) {
            // Other diagonal
            if (board[0][2] == mark && board[1][1] == mark && board[2][0] == mark) {
                return mark; // Win
            }
        }

        // Check for draw (board full)
        boolean boardFull = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') {
                    boardFull = false;
                    break;
                }
            }
            if (!boardFull) break;
        }

        if (boardFull) {
            return 'D'; // Draw
        }

        return ' '; // Game continues
    }
}