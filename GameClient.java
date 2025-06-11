import java.io.*;
import java.net.Socket;
import java.net.ConnectException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class GameClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String playerName;
    private GameGUI gui;
    private boolean connected = false;

    public GameClient(String serverAddress, int port) {
        connectToServer(serverAddress, port);
    }

    private void connectToServer(String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            System.out.println("Connected to server at " + serverAddress + ":" + port);
        } catch (ConnectException ce) {
            System.err.println("Server is not available: " + ce.getMessage());
            if (gui != null) {
                gui.showError("Cannot connect to server at " + serverAddress + ":" + port);
            } else {
                JOptionPane.showMessageDialog(null,
                        "Cannot connect to server at " + serverAddress + ":" + port,
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            System.err.println("Unable to connect to server: " + e.getMessage());
            if (gui != null) {
                gui.showError("Connection error: " + e.getMessage());
            } else {
                JOptionPane.showMessageDialog(null,
                        "Connection error: " + e.getMessage(),
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void setPlayerName(String name) {
        this.playerName = name;
        if (connected) {
            sendToServer("NAME " + name);
        }
    }

    public void setGUI(GameGUI gui) {
        this.gui = gui;
    }

    public BufferedReader getInputReader() {
        return in;
    }

    public void sendToServer(String message) {
        if (connected && out != null) {
            out.println(message);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        if (connected) {
            try {
                connected = false;
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Default values
            String serverAddress = "localhost";
            int port = 12345;

            // Allow command line arguments for server address and port
            if (args.length >= 1) {
                serverAddress = args[0];
            }
            if (args.length >= 2) {
                try {
                    port = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number. Using default: 12345");
                }
            }

            // If no command line arguments, ask for server information
            if (args.length == 0) {
                // Get server information from the user
                serverAddress = JOptionPane.showInputDialog(
                        null,
                        "Enter server IP address:",
                        "Server Connection",
                        JOptionPane.QUESTION_MESSAGE
                );

                if (serverAddress == null || serverAddress.trim().isEmpty()) {
                    serverAddress = "localhost";
                }

                String portStr = JOptionPane.showInputDialog(
                        null,
                        "Enter server port:",
                        "Server Connection",
                        JOptionPane.QUESTION_MESSAGE
                );

                if (portStr != null && !portStr.trim().isEmpty()) {
                    try {
                        port = Integer.parseInt(portStr.trim());
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid port number. Using default: 12345");
                    }
                }
            }

            GameClient client = new GameClient(serverAddress, port);

            if (client.isConnected()) {
                GameGUI gui = new GameGUI(client);
                client.setGUI(gui);
                gui.createAndShowGUI();
            } else {
                System.exit(1);
            }
        });
    }
}