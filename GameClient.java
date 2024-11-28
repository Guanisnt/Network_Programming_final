import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class GameClient extends JFrame {
    private static final int UDP_CONNECTION_PORT = 12345;
    private static final int UDP_MOVEMENT_PORT = 124;
    private static final int BUFSIZE = 1024;
    private static final int WIDTH = 1440;
    private static final int HEIGHT = 720;
    private GamePanel gamePanel;
    private int clientId = -1;
    private GameState gameState = new GameState();
    private DatagramSocket udpSocket; // UDP socket
    private DatagramPacket connectPacket; // 連線的封包
    private DatagramPacket sendPacket; // 送出的封包
    private InetAddress serverAddress; // server的IP

    public GameClient() {
        this.setTitle("hello");
        this.setSize(WIDTH, HEIGHT);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gamePanel = new GamePanel(); // 畫面
        this.add(gamePanel);

        try {
            udpSocket = new DatagramSocket();
            serverAddress = InetAddress.getByName("localhost"); // server的IP
            byte[] connectData = "connect".getBytes(); // 連線訊息
            connectPacket = new DatagramPacket(connectData, connectData.length, serverAddress, UDP_CONNECTION_PORT); // 連線封包
            udpSocket.send(connectPacket); // 連線
            new Thread(this::receiveUDPMessage).start(); // 接收server訊息

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if(clientId != -1) {
                        String movement = null;
                        switch (e.getKeyCode()) {
                            case KeyEvent.VK_LEFT: 
                                movement = clientId + ":LEFT"; 
                                break;
                            case KeyEvent.VK_RIGHT: 
                                movement = clientId + ":RIGHT"; 
                                break;
                            case KeyEvent.VK_UP: 
                                movement = clientId + ":JUMP"; 
                                break;
                            case KeyEvent.VK_SPACE: 
                                movement = clientId + ":IncG"; 
                                break;
                        }
                        
                        if(movement != null) {
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

    // 接收server訊息
    private void receiveUDPMessage() {
        byte[] receiveData = new byte[BUFSIZE];
        try {
            while(true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                udpSocket.receive(receivePacket); // 收送進來的封包
                String msg = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                processMsg(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 送出移動訊息
    private void sendUDPMovement(String movement) {
        try {
            System.out.println("Sending movement: " + movement);
            byte[] sendData = movement.getBytes();
            sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, UDP_MOVEMENT_PORT);
            udpSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); 
            gameState.draw(g); // 畫所有東西
        }
    }

    public void processMsg(String msg) {
        // System.out.println(msg);
        if(msg.startsWith("STATE:")) {
            String[] splits = msg.substring(6).split(";");
            for(String split : splits) {
                if(!split.isEmpty()) {
                    String[] playerInfo = split.split(",");
                    int id = Integer.parseInt(playerInfo[0]);
                    double x = Double.parseDouble(playerInfo[1]);
                    double y = Double.parseDouble(playerInfo[2]);
                    Color color = new Color(Integer.parseInt(playerInfo[3]));
                    double vX = Double.parseDouble(playerInfo[4]);
                    double vY = Double.parseDouble(playerInfo[5]);
                    boolean isAlive = Boolean.parseBoolean(playerInfo[6]);

                    if(!gameState.getPlayers().containsKey(id)) {  // 如果沒有這個玩家，就新增
                        gameState.addPlayer(id);
                    }
                    gameState.updatePlayer(id, x, y, color, vX, vY, isAlive); // 更新玩家資訊
                }
            }
            gamePanel.repaint(); // 重畫
        } else if(msg.startsWith("new_player:")) {
            int id = Integer.parseInt(msg.substring(11));
            if(clientId == -1) {
                clientId = id;
                System.out.println("New client id: " + clientId);
            }
            if(!gameState.getPlayers().containsKey(id)) {
                gameState.addPlayer(id);
            }
        } else if(msg.startsWith("PLAYERREMOVE:")) {
            int id = Integer.parseInt(msg.substring(13));
            gameState.removePlayer(id);
        }
    }

    public static void main(String[] args) {
        GameClient client = new GameClient();
        client.setVisible(true);
    }
}