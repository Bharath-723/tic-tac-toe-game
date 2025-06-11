import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;

public class GameGUI {
    private final GameClient client;
    private JFrame frame;
    private JButton[][] buttons = new JButton[3][3];
    private JTextArea chatArea;
    private JTextField chatInput;
    private JLabel statusLabel;
    private JButton restartButton;

    private String mySymbol = "";
    private boolean myTurn = false;
    private boolean gameOver = false;

    // Track the board state on the client side
    private char[][] boardState = new char[3][3];

    public GameGUI(GameClient client) {
        this.client = client;
        // Initialize board to empty spaces
        resetBoard();
    }

    private void resetBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                boardState[i][j] = ' ';
            }
        }
    }

    public void createAndShowGUI() {
        String name = JOptionPane.showInputDialog("Enter your name:");
        if (name == null || name.trim().isEmpty()) name = "Player";
        client.setPlayerName(name);

        frame = new JFrame("Tic-Tac-Toe");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Status panel at the top
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Waiting for opponent...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusPanel.add(statusLabel, BorderLayout.CENTER);

        // Restart button
        restartButton = new JButton("New Game");
        restartButton.setEnabled(false);
        restartButton.addActionListener(e -> {
            client.sendToServer("RESTART");
            chatArea.append("System: Requesting new game...\n");
            restartButton.setEnabled(false);
        });
        statusPanel.add(restartButton, BorderLayout.EAST);

        frame.add(statusPanel, BorderLayout.NORTH);

        // Game board in the center
        JPanel board = new JPanel(new GridLayout(3, 3));
        board.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        Font buttonFont = new Font("Arial", Font.BOLD, 60);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                JButton button = new JButton("");
                button.setFont(buttonFont);
                button.setPreferredSize(new Dimension(100, 100));
                final int r = row;
                final int c = col;
                button.addActionListener(e -> {
                    if (myTurn && !gameOver && button.getText().isEmpty()) {
                        client.sendToServer("Move " + r + "," + c);
                    }
                });
                buttons[row][col] = button;
                board.add(button);
            }
        }

        frame.add(board, BorderLayout.CENTER);

        // Chat panel on the right
        chatArea = new JTextArea(10, 20);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);

        chatInput = new JTextField();
        JButton sendButton = new JButton("Send");

        JPanel chatBottom = new JPanel(new BorderLayout());
        chatBottom.add(chatInput, BorderLayout.CENTER);
        chatBottom.add(sendButton, BorderLayout.EAST);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));
        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(chatBottom, BorderLayout.SOUTH);

        frame.add(chatPanel, BorderLayout.EAST);

        // Event listeners for chat
        sendButton.addActionListener(e -> sendChat());
        chatInput.addActionListener(e -> sendChat());

        // Set frame properties
        frame.setSize(600, 400);
        frame.setMinimumSize(new Dimension(500, 350));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Only start listening if we're connected
        if (client.isConnected()) {
            listenToServer();
        }
    }

    private void sendChat() {
        String text = chatInput.getText().trim();
        if (!text.isEmpty()) {
            client.sendToServer("Chat " + text);
            chatInput.setText("");
        }
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void listenToServer() {
        new Thread(() -> {
            try {
                BufferedReader in = client.getInputReader();
                String line;
                while ((line = in.readLine()) != null) {
                    final String message = line;
                    SwingUtilities.invokeLater(() -> processServerMessage(message));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Disconnected from server.");
                    disableAllButtons();
                });
            }
        }).start();
    }

    private void processServerMessage(String message) {
        if (message.startsWith("Move ")) {
            String[] parts = message.split(" ");
            if (parts.length == 3) {
                String symbol = parts[1];
                String[] coords = parts[2].split(",");
                int row = Integer.parseInt(coords[0]);
                int col = Integer.parseInt(coords[1]);

                // Update UI
                buttons[row][col].setText(symbol);

                // Update client-side board state
                boardState[row][col] = symbol.charAt(0);
            }
        } else if (message.startsWith("Chat ")) {
            chatArea.append(message.substring(5) + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        } else if (message.equals("Your turn")) {
            myTurn = true;
            statusLabel.setText("Your turn");
        } else if (message.equals("Opponent's turn")) {
            myTurn = false;
            statusLabel.setText("Opponent's turn");
        } else if (message.equals("You are X") || message.equals("You are O")) {
            mySymbol = message.endsWith("X") ? "X" : "O";
            statusLabel.setText("You are playing as " + mySymbol);
        } else if (message.startsWith("GAME_OVER")) {
            gameOver = true;
            myTurn = false;
            statusLabel.setText(message.substring(10));
            restartButton.setEnabled(true);
        } else if (message.equals("RESET_GAME")) {
            // Reset the game board UI
            resetUIForNewGame();
            chatArea.append("System: Starting new game!\n");
        }
    }

    private void resetUIForNewGame() {
        // Reset the UI elements for a new game
        resetBoard();

        // Clear all buttons
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                buttons[row][col].setText("");
                buttons[row][col].setEnabled(true);
            }
        }

        // Reset game state
        gameOver = false;
        restartButton.setEnabled(false);
    }

    private void disableAllButtons() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                buttons[row][col].setEnabled(false);
            }
        }
    }
}