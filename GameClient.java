import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class GameClient extends JFrame {
    private static final int PORT = 123;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private GamePanel gamePanel;
    private Socket socket;
    private DataOutputStream dos;
    private BufferedReader bin;
    private int clientId = -1;
    private GameState gameState = new GameState();
    private PrintWriter out;

    public GameClient() {
        this.setTitle("hello");
        this.setSize(WIDTH, HEIGHT);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gamePanel = new GamePanel(); // 畫面
        this.add(gamePanel);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT: sendInput("LEFT"); break;
                    case KeyEvent.VK_RIGHT: sendInput("RIGHT"); break;
                    case KeyEvent.VK_UP: sendInput("JUMP"); break;
                }
            }
        });

        // connectServer("localhost", PORT);
        setFocusable(true);

        try {
            socket = new Socket("localhost", PORT);
            // dos = new DataOutputStream(socket.getOutputStream());
            out = new PrintWriter(socket.getOutputStream(), true);
            bin = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(this::receiveMessage).start();
            setFocusable(true);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void sendInput(String input) {
        out.println(input);
        // try {
        //     dos.writeBytes(input + "\n");
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
    }

    public class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); 
            gameState.draw(g); // 畫所有東西
        }
    }

    public void receiveMessage() {
        try {
            String inputMsg;
            while((inputMsg = bin.readLine()) != null) {
                // System.out.println("Server: " + inputMsg);
                processMsg(inputMsg);
            }
        } catch (IOException e) {
            e.printStackTrace();
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