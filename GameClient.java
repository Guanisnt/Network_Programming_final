import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

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
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (inChatMode) {
                            String message = JOptionPane.showInputDialog("Enter your message:");
                            if (message != null && !message.isEmpty()) {
                                sendChatMessage(message);
                            }
                            inChatMode = false;
                        } else {
                            inChatMode = true;
                        }
                    } else if (!inChatMode && clientId != -1) {
                        String movement = null;
                        switch (e.getKeyCode()) {
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
        } catch (IOException e) {
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
            while ((msg = tcpReader.readLine()) != null) {
                synchronized (chatMessages) {
                    if (chatMessages.size() >= 6) {
                        chatMessages.remove(0);
                    }
                    chatMessages.add(msg);
                }
                SwingUtilities.invokeLater(() -> {  //使用 SwingUtilities.invokeLater() 確保重繪在 Event Dispatch Thread 中進行
                    gamePanel.repaint();
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 接收伺服器訊息
    private void receiveUDPMessage() {
        byte[] receiveData = new byte[BUFSIZE];
        try {
            while (true) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 處理伺服器訊息
    private void processMsg(String msg) {
        if (msg.startsWith("STATE:")) {
            String[] splits = msg.substring(6).split(";");
            for (String split : splits) {
                if (!split.isEmpty()) {
                    String[] playerInfo = split.split(",");
                    int id = Integer.parseInt(playerInfo[0]);
                    double x = Double.parseDouble(playerInfo[1]);
                    double y = Double.parseDouble(playerInfo[2]);
                    Color color = new Color(Integer.parseInt(playerInfo[3]));
                    double vX = Double.parseDouble(playerInfo[4]);
                    double vY = Double.parseDouble(playerInfo[5]);
                    boolean isAlive = Boolean.parseBoolean(playerInfo[6]);

                    if (!gameState.getPlayers().containsKey(id)) {
                        gameState.addPlayer(id);
                    }
                    gameState.updatePlayer(id, x, y, color, vX, vY, isAlive);
                }
            }
            gamePanel.repaint();
        } else if (msg.startsWith("new_player:")) {
            int id = Integer.parseInt(msg.substring(11));
            if (clientId == -1) {
                clientId = id;
            }
            if(playerId == -1){
                playerId = id;
            }
            if (!gameState.getPlayers().containsKey(id)) {
                gameState.addPlayer(id);
            }
        } else if (msg.startsWith("PLAYERREMOVE:")) {
            int id = Integer.parseInt(msg.substring(13));
            gameState.removePlayer(id);
        }
    }

    // 繪製遊戲畫面
    public class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            gameState.draw(g);
            drawChat(g);
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
        
            synchronized (chatMessages) {
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
        GameClient client = new GameClient();
        client.setVisible(true);
    }
}
