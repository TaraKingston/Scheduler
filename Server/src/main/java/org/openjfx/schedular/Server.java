package org.openjfx.schedular;


import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws Exception {

        ScheduleManager manager = new ScheduleManager();
        ServerSocket serverSocket = new ServerSocket(6969);
        System.out.println("Server started on port 6969, waiting for client...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected!");
            new Thread(new ClientHandler(clientSocket, manager)).start();
        }
    }
}
