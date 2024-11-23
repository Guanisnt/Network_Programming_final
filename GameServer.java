import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {
    private static final int PORT = 123;
    private ServerSocket ss;
    private ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private GameState gameState = new GameState();
    private int nextId = 1;

    public GameServer() {
        try {
            ss = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            System.out.println("waiting for client to connect...");
            new Thread(this::gameloop).start(); // 開始遊戲迴圈
            while(true) {
                Socket socket = ss.accept();
                int clientId = nextId++;
                ClientHandler client = new ClientHandler(clientId, socket, this);
                clients.put(clientId, client);
                sendToNewClient(client); // 送給新client所有玩家資訊，不然新家進來的看不到其他玩家
                gameState.addPlayer(clientId); // 新增玩家
                broadcastMessage("new_player" + clientId);
                new Thread(client).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void gameloop() {
        while(true) {
            gameState.update();
            broadcastGameState(); // 廣播遊戲狀態給所有client
            try {
                Thread.sleep(16); // 睡一下不然太快，16ms是60fps
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendToNewClient(ClientHandler client) {
        for(Sprite player : gameState.getPlayers().values()) {
            client.sendMsg("PLAYERADD:" + player.getId() + "," + player.getX() + "," + player.getY() + "," + player.getColor().getRGB());
        }
    }

    // 廣播訊息給所有client
    public void broadcastMessage(String msg) {
        for(ClientHandler client : clients.values()) {
            client.sendMsg(msg);
        }
    }

    // 把狀態用成字串，再用broadcastMessage廣播給所有client
    public void broadcastGameState() {
        StringBuilder state = new StringBuilder("STATE:");
        for(Sprite player : gameState.getPlayers().values()) { // <id, sprite>
            state.append(player.getId()).append(",")
                 .append(player.getX()).append(",")
                 .append(player.getY()).append(",")
                 .append(player.getColor().getRGB()).append(",")
                 .append(player.getVelocityX()).append(",")
                 .append(player.getVelocityY()).append(",")
                 .append(player.isAlive()).append(";");
        }
        broadcastMessage(state.toString());
    }

    public void handleInput(int clientId, String input) {
        Sprite player = gameState.getPlayers().get(clientId);
        if(player != null && player.isAlive()) {
            switch(input) {
                case "LEFT": player.moveLeft(); break;
                case "RIGHT": player.moveRight(); break;
                case "JUMP": player.jump(); break;
            }
        }
    }

    public void removeClient(int clientId) {
        clients.remove(clientId);
        gameState.removePlayer(clientId);
        broadcastMessage("PLAYERREMOVE:" + clientId);
    }

    public static void main(String[] args) {
        new GameServer();
        // new GameServer().start();
    }
}
