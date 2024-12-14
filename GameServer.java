import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {
    private static final int UDP_CONNECTION_PORT = 12345;
    private static final int UDP_MOVEMENT_PORT = 124;
    private DatagramSocket udpConnectionSocket; // 用來接收client連線
    private DatagramSocket udpMovementSocket; // 用來接收client移動封包

    private ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private GameState gameState = new GameState();
    private int nextId = 1;
    private ConcurrentHashMap<Integer, ChattingRoomHandler> chatClients = new ConcurrentHashMap<>();// 聊天室的client


    public GameServer() {
        try {
            udpConnectionSocket = new DatagramSocket(UDP_CONNECTION_PORT);
            udpMovementSocket = new DatagramSocket(UDP_MOVEMENT_PORT);
            System.out.println("Server started on port " + UDP_CONNECTION_PORT);
            System.out.println("waiting for client to connect...");
            new Thread(this::receiveUDPConnections).start(); // client連線
            new Thread(this::receiveUDPMovements).start(); // client移動
            new Thread(this::gameloop).start(); // 開始遊戲迴圈
            new Thread(this::tcpgamechattingroom).start(); // 開始遊戲聊天室
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 接收client連線
    private void receiveUDPConnections() {
        System.out.println("connected");
        byte[] receiveData = new byte[1024];
        while(true) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                udpConnectionSocket.receive(receivePacket);
                System.out.println("Received connection from " + receivePacket.getAddress() + ":" + receivePacket.getPort());
                // 處理新的client，要放到ClientHandler裡面處理
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                int ClientId = nextId++;
                ClientHandler client = new ClientHandler(ClientId, clientAddress, clientPort, this, udpConnectionSocket);
                clients.put(ClientId, client);

                // 送給新client所有玩家資訊，不然新家進來的看不到其他玩家
                String welcomeMsg = "new_player:" + ClientId;
                client.sendMsg(welcomeMsg);

                // 新增玩家
                gameState.addPlayer(ClientId);
                broadcastMessage(welcomeMsg);

                // 每個client都有自己的一個ClientHandler(簡報的ServerThread)
                new Thread(client).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 開始遊戲聊天室
    private void tcpgamechattingroom() {
        try {
            ServerSocket serverSocket = new ServerSocket(12346);
            System.out.println("Chat server started on port 12346");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                int playerId = nextId;
                ChattingRoomHandler chatHandler = new ChattingRoomHandler(clientSocket, playerId, this);
                chatClients.put(playerId, chatHandler);
                new Thread(chatHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void broadcastChatMessage(String message) {
        for (ChattingRoomHandler chatHandler : chatClients.values()) {
            chatHandler.sendMessage(message);
        }
    }

    // 處理每個玩家的聊天訊息並廣播給所有人
    class ChattingRoomHandler implements Runnable {
        private Socket socket;
        private int playerId;
        private GameServer server;
        private BufferedWriter writer;
        private BufferedReader reader;
    
        public ChattingRoomHandler(Socket socket, int clientId, GameServer server) {
            this.socket = socket;
            this.playerId = playerId;
            this.server = server;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
        @Override
        public void run() {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println("Received from client " + message);
                    server.broadcastChatMessage("Player" + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                server.chatClients.remove(playerId);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    
        public void sendMessage(String message) {
            try {
                writer.write(message);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    


    // 接收client操作指令
    private void receiveUDPMovements() {
        byte[] receiveData = new byte[1024];
        while(true) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                udpMovementSocket.receive(receivePacket);
                String movementData = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                System.out.println("Received movement: " + movementData);
                
                // Format: "clientId:MOVEMENT"
                String[] parts = movementData.split(":");
                if(parts.length == 2) {
                    int clientId = Integer.parseInt(parts[0]);
                    String movement = parts[1];
                    handleInput(clientId, movement);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                case "IncG": player.increaseGravity(); break;
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
    }
}
