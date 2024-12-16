import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class GameClient extends JFrame {
    private static final int UDP_CONNECTION_PORT = 12345;
    private static final int UDP_MOVEMENT_PORT = 124;
    private static final int BUFSIZE = 1024;
    private static final int WIDTH = 1440;
    private static final int HEIGHT = 720;

    private GamePanel gamePanel;
    private int clientId = -1;
    private GameState gameState = new GameState();
    private DatagramSocket udpSocket;
    private InetAddress serverAddress;

    private Socket tcpSocket;
    private BufferedWriter tcpWriter;
    private BufferedReader tcpReader;
    private boolean inChatMode = false;
    private final List<String> chatMessages = new ArrayList<>();
    private int playerId = -1;
    private Map<Integer, Integer> playerScores = new HashMap<>();

    public GameClient() {
        this.setTitle("Game with Chat");
        this.setSize(WIDTH, HEIGHT);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gamePanel = new GamePanel();
        gamePanel.setVisible(true);
        this.add(gamePanel);

        try {
            // 設置 TCP 和 UDP 連線
            serverAddress = InetAddress.getByName("localhost");
            tcpSocket = new Socket(serverAddress, 12346);
            tcpWriter = new BufferedWriter(new OutputStreamWriter(tcpSocket.getOutputStream()));
            tcpReader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            udpSocket = new DatagramSocket();

            // 啟動接收訊息的執行緒
            new Thread(this::receiveTCPMessages).start();
            new Thread(this::receiveUDPMessage).start();

            // 發送 UDP 連線訊息
            byte[] connectData = "connect".getBytes();
            DatagramPacket connectPacket = new DatagramPacket(connectData, connectData.length, serverAddress, UDP_CONNECTION_PORT);
            udpSocket.send(connectPacket);

            // 設置鍵盤事件監聽
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if(inChatMode) {
                            String message = JOptionPane.showInputDialog("Enter your message:");
                            if(message != null && !message.isEmpty()) {
                                sendChatMessage(message);
                            }
                            inChatMode = false;
                        } else {
                            inChatMode = true;
                        }
                    } else if(!inChatMode && clientId != -1) {
                        String movement = null;
                        switch(e.getKeyCode()) {
                            case KeyEvent.VK_LEFT -> movement = clientId + ":LEFT";
                            case KeyEvent.VK_RIGHT -> movement = clientId + ":RIGHT";
                            case KeyEvent.VK_UP -> movement = clientId + ":JUMP";
                            case KeyEvent.VK_SPACE -> movement = clientId + ":IncG";
                        }
                        if (movement != null) {
                            sendUDPMovement(movement);
                        }
                    }
                }
            });

            setFocusable(true);
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // 發送聊天室訊息
    private void sendChatMessage(String message) {
        try {
            tcpWriter.write(playerId + " : " + message); 
            tcpWriter.newLine();
            tcpWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 接收聊天室訊息
    private void receiveTCPMessages() {
        try {
            String msg;
            while((msg = tcpReader.readLine()) != null) {
                synchronized(chatMessages) {
                    if (chatMessages.size() >= 7) {
                        chatMessages.remove(0);
                    }
                    chatMessages.add(msg);
                }
                SwingUtilities.invokeLater(() -> {  //使用 SwingUtilities.invokeLater() 確保重繪在 Event Dispatch Thread 中進行
                    gamePanel.repaint();
                });
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    // 接收伺服器訊息
    private void receiveUDPMessage() {
        byte[] receiveData = new byte[BUFSIZE];
        try {
            while(true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                udpSocket.receive(receivePacket);
                String msg = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                processMsg(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 發送移動指令
    private void sendUDPMovement(String movement) {
        try {
            byte[] sendData = movement.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, UDP_MOVEMENT_PORT);
            udpSocket.send(sendPacket);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    // 處理伺服器訊息
    private void processMsg(String msg) {
        if(msg.startsWith("STATE:")) {
            String[] splits = msg.substring(6).split(";");
            for(String split : splits) {
                if(!split.isEmpty()) {
                    // String[] playerInfo = split.split(",");
                    // int id = Integer.parseInt(playerInfo[0]);
                    // double x = Double.parseDouble(playerInfo[1]);
                    // double y = Double.parseDouble(playerInfo[2]);
                    // Color color = new Color(Integer.parseInt(playerInfo[3]));
                    // double vX = Double.parseDouble(playerInfo[4]);
                    // double vY = Double.parseDouble(playerInfo[5]);
                    // boolean isAlive = Boolean.parseBoolean(playerInfo[6]);
                    String[] playerInfo = split.split(",");
                    int id = Integer.parseInt(playerInfo[0]);
                    double x = Double.parseDouble(playerInfo[1]);
                    double y = Double.parseDouble(playerInfo[2]);
                    Color color = new Color(Integer.parseInt(playerInfo[3]));
                    double vX = Double.parseDouble(playerInfo[4]);
                    double vY = Double.parseDouble(playerInfo[5]);
                    boolean isAlive = Boolean.parseBoolean(playerInfo[6]);
                    int score = Integer.parseInt(playerInfo[7]);

                    if(!gameState.getPlayers().containsKey(id)) {
                        gameState.addPlayer(id);
                    }
                    gameState.updatePlayer(id, x, y, color, vX, vY, isAlive);
                    playerScores.put(id, score); // 更新分數
                }
            }
            gamePanel.repaint();
        } else if(msg.startsWith("new_player:")) {
            int id = Integer.parseInt(msg.substring(11));
            if(clientId == -1) {
                clientId = id;
            }
            if(playerId == -1){
                playerId = id;
            }
            if(!gameState.getPlayers().containsKey(id)) {
                gameState.addPlayer(id);
            }
        } else if(msg.startsWith("PLAYERREMOVE:")) {
            int id = Integer.parseInt(msg.substring(13));
            gameState.removePlayer(id);
        } else if (msg.startsWith("GAME_OVER:")) {
            gamePanel.repaint();
            int winnerId = Integer.parseInt(msg.substring(10));
            JOptionPane.showMessageDialog(this, "Game Over! Winner: Player " + winnerId);
            System.exit(0);
        } else if (msg.startsWith("GAME_OVER_MULTIPLE:")) {
            // 結束前畫出最終分數
            gamePanel.repaint();
            String[] winnerIds = msg.substring(19).split(",");
            JOptionPane.showMessageDialog(this, "Game Over! Winners: " + String.join(", ", winnerIds));
            System.exit(0);
        }
    }

    // 繪製遊戲畫面
    public class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            gameState.draw(g);
            drawChat(g);
            drawScores(g);
        }

        private void drawScores(Graphics g) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 18));
            int y = 20;
            for(Map.Entry<Integer, Integer> entry : playerScores.entrySet()) {
                g.drawString("Player " + entry.getKey() + " Score: " + entry.getValue(), 10, y);
                y += 20;
            }
        }

        private void drawChat(Graphics g) {
            int chatWidth = 400; // 聊天框寬度
            int chatHeight = 140; // 聊天框高度
            int chatX = 10; // 聊天框 X 座標
            int chatY = getHeight() - chatHeight - 20; // 聊天框 Y 座標 (左下角)
        
            // 畫聊天框背景
            g.setColor(new Color(50, 50, 50, 150)); // 灰黑色半透明背景
            g.fillRect(chatX, chatY, chatWidth, chatHeight);
        
            // 設定字型與顏色
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 14));
        
            // 繪製聊天室標題
            g.drawString("Chat Messages:", chatX + 10, chatY + 20);
        
            // 繪製聊天訊息
            int textX = chatX + 10; // 訊息文字的 X 座標
            int textY = chatY + 40; // 第一行文字的起始 Y 座標
            int lineHeight = 15; // 每行文字的高度
        
            synchronized(chatMessages) {
                for (String msg : chatMessages) {
                    if(!msg.isEmpty()){
                       
                        g.drawString(msg, textX, textY);
                        textY += lineHeight;
                    }
        
                    // 如果超出聊天框範圍，則不再繪製
                    if (textY > chatY + chatHeight - 10) {
                        break;
                    }
                }
            }
        }
        
    }

    public static void main(String[] args) {
        // UI
        JFrame startWindow = new JFrame("GameBall");
        startWindow.setSize(1440, 720);
        startWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startWindow.setLayout(new BorderLayout());

        // 設置背景面板
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // 畫深色背景與網格線
                g.setColor(new Color(30, 30, 30)); // 深灰色背景
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(new Color(70, 70, 70)); // 深灰網格線
                for (int i = 0; i < getWidth(); i += 50) {
                    g.drawLine(i, 0, i, getHeight());
                }
                for (int j = 0; j < getHeight(); j += 50) {
                    g.drawLine(0, j, getWidth(), j);
                }
            }
        };
        backgroundPanel.setLayout(new BorderLayout());

        // 遊戲名稱標題
        JLabel gameTitle = new JLabel("GAMEBALL", SwingConstants.CENTER);
        gameTitle.setFont(new Font("Arial", Font.BOLD, 60));
        gameTitle.setForeground(new Color(180, 180, 180)); // 淡灰色
        gameTitle.setBorder(BorderFactory.createEmptyBorder(180, 0, 80, 0));

        // 開始按鈕
        JButton startButton = new JButton("START GAME");
        startButton.setFont(new Font("Arial", Font.BOLD, 24));
        startButton.setBackground(new Color(50, 50, 50)); // 深灰色背景
        startButton.setForeground(new Color(200, 200, 200)); // 淡灰文字
        startButton.setFocusPainted(false);
        startButton.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 3)); // 中灰邊框
        startButton.setPreferredSize(new Dimension(200, 50));   // 設定按鈕大小

        // 加入滑鼠懸停效果
        startButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                startButton.setBackground(new Color(70, 70, 70)); // 滑鼠移入變淺灰
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                startButton.setBackground(new Color(50, 50, 50)); // 滑鼠移出恢復
            }
        });

        // 按鈕點擊事件
        startButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                GameClient client = new GameClient(); // 啟動遊戲客戶端
                client.setVisible(true);
            });
            startWindow.dispose(); // 關閉開始畫面
        });

        // 遊戲規則
        JLabel rulesLabel = new JLabel(
            "<html><div style='text-align: center; color: #CCCCCC; font-size: 18px;'>" // 暗灰字體
            + "<p><strong>遊戲規則:</strong></p>"
            + "<p>1. 控制角色跳躍與移動，存活到最後。</p>"
            + "<p>2. 按下上鍵可以跳躍，左右鍵移動方向。</p>"
            + "<p>3. 連跳技巧是獲勝的關鍵！</p>"
            + "</div></html>"
        );
        rulesLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rulesLabel.setBorder(BorderFactory.createEmptyBorder(20, 50, 100, 50));

        // 添加元件到背景面板
        backgroundPanel.add(gameTitle, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false); // 透明背景
        centerPanel.add(startButton);
        backgroundPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false); // 透明背景
        bottomPanel.add(rulesLabel);
        backgroundPanel.add(bottomPanel, BorderLayout.SOUTH);

        // 加入背景面板到視窗
        startWindow.add(backgroundPanel);
        startWindow.setLocationRelativeTo(null);
        startWindow.setVisible(true);
    }
}
