/*處理client端的資訊，跟上課教的ServerThread概念一樣 */
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private int clientId;
    private GameServer server;
    private DatagramSocket udpSocket;
    private InetAddress clientAddress;
    private int clientPort;

    public ClientHandler(int clientId, InetAddress clientAddress, int clientPort, GameServer server, DatagramSocket udpSocket) {
        this.clientId = clientId;
        this.server = server;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.udpSocket = udpSocket;
    }

    // server廣播訊息給所有client會用到，用UDP
    public void sendMsg(String msg) {
        try {
            byte[] sendData = msg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            udpSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] receiveData = new byte[1024];
        try {
            while(true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                udpSocket.receive(receivePacket);
                String inputLine = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim(); // trim可以刪掉字串頭尾的空白
                System.out.println("Client " + clientId + " sent: " + inputLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server.removeClient(clientId);
        }
    }
}
