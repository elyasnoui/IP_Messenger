package server;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ServerHandler implements Runnable {
    private Socket server;
    private BufferedReader in;
    private JList messageList;
    private ArrayList<String> chatLog;
    private JScrollPane scrollPane;

    public ServerHandler(Socket server, JList messageList, ArrayList<String> chatLog, JScrollPane scrollPane) throws IOException {
        this.server = server;
        this.messageList = messageList;
        this.chatLog = chatLog;
        this.scrollPane = scrollPane;
        in = new BufferedReader(new InputStreamReader(server.getInputStream()));
    }

    @Override
    public void run() {
        try {
            String response = "";
            while (true) {
                response = in.readLine();

                if (response == null) continue;

                chatLog.add(response);
                messageList.setListData(chatLog.toArray());
            }
        } catch (IOException e) { e.printStackTrace(); }
        finally {
            try { in.close(); }
            catch (IOException e) { e.printStackTrace(); }
        }
    }
}
