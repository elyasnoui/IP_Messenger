package client;

import server.ServerHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public class ClientMessenger {
    private JPanel panel;
    private JButton sendButton;
    private JList messageList;
    private JTextField messageField;
    private JButton exitButton;
    private ArrayList<String> chatLog = new ArrayList<>();

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientMessenger(Socket socket, String username) throws IOException {
        this.socket = socket;
        out = new PrintWriter(socket.getOutputStream(), true);
        //in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ServerHandler serverHandler = new ServerHandler(socket, messageList, chatLog);

        new Thread(serverHandler).start();

        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    out.println(username+": "+messageField.getText());
                    messageField.setText("");

                }
            }
        });
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                out.println("3x1t");
                System.exit(0);
            }
        });
    }

    public JPanel getPanel() {
        return panel;
    }
}
