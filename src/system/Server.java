package system;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Elyas, Noui, elyas.noui@city.ac.uk [190053026]
 * @version Java SE 15
 */

final class Server {
    public static final int port = 20111;  // Default port requested by module

    private static final ArrayList<ClientHandler> clients = new ArrayList<>();
    public static boolean run = true;

    // This limits the amount of clients that can connect, editing the parameter can increase/decrease that amount
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    private static void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);  // Opens a server using localhost

        // Listens for clients connecting and creates a Thread for them to communicate
        while (run) {
            Socket clientSocket = serverSocket.accept();
            PrintWriter clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
            clientOut.println("Accepted");
            ClientHandler clientLane = new ClientHandler(clientSocket, clients);
            clients.add(clientLane);

            executorService.execute(clientLane);
        }
    }

    public static void main(String[] args) throws IOException {
        start();
    }
}
