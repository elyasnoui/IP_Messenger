package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private ArrayList<ClientHandler> clients;

    public ClientHandler(Socket client, ArrayList<ClientHandler> clients) throws IOException {
        this.client = client;
        this.clients = clients;
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintWriter(client.getOutputStream(), true);
    }

    @Override
    public void run() {
        String message = "";
        System.out.println("[SERVER] Client joined...");
        while (!"3x1t".equalsIgnoreCase(message)) {
            try {
                message = in.readLine();

                if (message == null) continue;

                //out.println(message);

                broadcast(message);

            } catch (IOException e) { e.printStackTrace(); }
        }
        try {
            System.out.println("here");
            in.close();
            out.close();
            client.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcast(String message) {
        for (ClientHandler c : clients)
            c.out.println(message);
    }
}
