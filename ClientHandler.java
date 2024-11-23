/*處理client端的資訊，跟上課教的ServerThread概念一樣 */
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private int clientId;
    private Socket socket;
    private GameServer server;
    private DataOutputStream dos;
    private BufferedReader bin;
    private PrintWriter out;

    public ClientHandler(int clientId, Socket socket, GameServer server) {
        this.clientId = clientId;
        this.socket = socket;
        this.server = server;
        try {
            // dos = new DataOutputStream(socket.getOutputStream());
            out = new PrintWriter(socket.getOutputStream(), true);
            bin = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // server廣播訊息給所有client會用到
    public void sendMsg(String msg) {
        out.println(msg);
        // try {
        //     dos.writeBytes(msg + "\n");
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }
    }

    @Override
    public void run() {
        try {
            String inputLine; // client端傳來的訊息
            while((inputLine = bin.readLine()) != null) {
                System.out.println("Client " + clientId + ": " + inputLine);
                server.handleInput(clientId, inputLine); // 呼叫server的handleInput處理
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Client " + clientId + " disconnected");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            server.removeClient(clientId); // 移除client
        }
    }
}
