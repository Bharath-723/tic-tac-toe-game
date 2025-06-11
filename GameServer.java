import java.io.*;
import java.net.*;

public class GameServer {
    // Game board to track state
    private static char[][] board = new char[3][3];
    private static int port = 12345;

    public static void main(String[] args) throws IOException {
        // Parse command line arguments for port
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: 12345");
            }
        }

        // Initialize board to empty
        resetBoard();

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port + "...");
        System.out.println("Server IP: " + InetAddress.getLocalHost().getHostAddress());

        try {
            while (true) {
                Socket socket1 = serverSocket.accept();
                System.out.println("Player X connected from " + socket1.getInetAddress());
                PlayerHandler playerX = new PlayerHandler(socket1, 'X', board);

                Socket socket2 = serverSocket.accept();
                System.out.println("Player O connected from " + socket2.getInetAddress());
                PlayerHandler playerO = new PlayerHandler(socket2, 'O', board);

                playerX.setOpponent(playerO);
                playerO.setOpponent(playerX);

                playerX.start();
                playerO.start();

                // Inform players of their symbols
                playerX.sendMessage("You are X");
                playerO.sendMessage("You are O");

                // Start the game
                playerX.sendMessage("Your turn");
                playerO.sendMessage("Opponent's turn");

                // Create a game monitor thread to handle restart requests
                new GameMonitorThread(playerX, playerO).start();
            }
        } finally {
            serverSocket.close();
            System.out.println("Server shutdown.");
        }
    }

    public static void resetBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
            }
        }
    }

    // Inner class to monitor game state and handle restarts
    private static class GameMonitorThread extends Thread {
        private PlayerHandler playerX;
        private PlayerHandler playerO;

        public GameMonitorThread(PlayerHandler playerX, PlayerHandler playerO) {
            this.playerX = playerX;
            this.playerO = playerO;
        }

        @Override
        public void run() {
            try {
                // Wait for both players to be ready for a new game
                while (!playerX.isDisconnected() && !playerO.isDisconnected()) {
                    if (playerX.wantsRestart() && playerO.wantsRestart()) {
                        // Reset the game
                        resetBoard();
                        playerX.resetGame();
                        playerO.resetGame();

                        // Start a new game
                        playerX.sendMessage("RESET_GAME");
                        playerO.sendMessage("RESET_GAME");

                        // X always starts
                        playerX.sendMessage("Your turn");
                        playerO.sendMessage("Opponent's turn");
                    }

                    // Sleep to prevent CPU hogging
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                System.out.println("Game monitor interrupted");
            }
        }
    }
}