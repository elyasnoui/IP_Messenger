package server;

import client.ClientHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int port = 20111;
    private static ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter clientOut;
    private BufferedReader clientIn;

    private static ArrayList<ClientHandler> clients = new ArrayList<>();
    private static ExecutorService executorService = Executors.newFixedThreadPool(2);

    public Server() throws IOException {
        serverSocket = new ServerSocket(port);

        while (true) {
            clientSocket = serverSocket.accept();
            clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
            clientOut.println("Accepted");
            ClientHandler clientLane = new ClientHandler(clientSocket, clients);
            clients.add(clientLane);

            executorService.execute(clientLane);
        }
    }

    public static void main(String[] args) throws IOException {
        new Server();
    }
}
