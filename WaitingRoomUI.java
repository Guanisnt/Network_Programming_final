import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;

public class WaitingRoomUI extends JFrame {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private JList<String> playerList;
    private DefaultListModel<String> playerListModel;
    private JButton startGameButton;
    private JLabel statusLabel;
    private GameClient gameClient;
    private boolean isHost = false;

    public WaitingRoomUI() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Multiplayer Game - Waiting Room");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Player List Panel
        JPanel playerListPanel = new JPanel(new BorderLayout());
        playerListModel = new DefaultListModel<>();
        playerList = new JList<>(playerListModel);
        playerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playerListPanel.add(new JScrollPane(playerList), BorderLayout.CENTER);
        playerListPanel.setBorder(BorderFactory.createTitledBorder("Connected Players"));

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        startGameButton = new JButton("Start Game");
        startGameButton.setEnabled(false);
        buttonPanel.add(startGameButton);

        // Status Panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statusLabel = new JLabel("Waiting for players to join...");
        statusPanel.add(statusLabel);

        // Add components to frame
        add(playerListPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(statusPanel, BorderLayout.NORTH);

        // Start Game Button Listener
        startGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isHost) {
                    startGame();
                }
            }
        });
    }

    // Method to connect to the game server
    public void connectToServer() {
        try {
            gameClient = new GameClient() {
                @Override
                public void processMsg(String msg) {
                    super.processMsg(msg);
                    
                    // Handle new player connections
                    if (msg.startsWith("new_player:")) {
                        int id = Integer.parseInt(msg.substring(11));
                        SwingUtilities.invokeLater(() -> {
                            playerListModel.addElement("Player " + id);
                            
                            // First player to connect is the host
                            if (playerListModel.size() == 1) {
                                isHost = true;
                                startGameButton.setEnabled(true);
                                statusLabel.setText("You are the host. Click 'Start Game' when ready.");
                            }
                        });
                    }
                    // Handle player removal
                    else if (msg.startsWith("PLAYERREMOVE:")) {
                        int id = Integer.parseInt(msg.substring(13));
                        SwingUtilities.invokeLater(() -> {
                            playerListModel.removeElement("Player " + id);
                        });
                    }
                }
            };
            
            // Hide waiting room and show game client when game starts
            gameClient.setVisible(false);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Failed to connect to server: " + e.getMessage(), 
                "Connection Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // Method to start the game
    private void startGame() {
        // Notify server that game is starting (you might want to implement this)
        // For now, just switch to game client
        dispose(); // Close waiting room
        gameClient.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WaitingRoomUI waitingRoom = new WaitingRoomUI();
            waitingRoom.setVisible(true);
            waitingRoom.connectToServer();
        });
    }
}