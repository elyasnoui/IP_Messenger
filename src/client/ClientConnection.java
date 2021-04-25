package client;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientConnection {
    private JTextField ipAddressField, portField, usernameField;
    private JButton connectButton;
    private JPanel panel;
    private JFrame frame;

    private String username, ip;
    private int port;

    private final String ipRegex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private final String portRegex = "[0-9]{1,5}";
    private final String usernameRegex = ".{1,10}";
    public static final LineBorder borderError = new LineBorder(Color.RED, 1);

    public static void main(String[] args) {
        ClientConnection client = new ClientConnection();

        client.frame = new JFrame("Client");
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.add(client.panel);
        client.frame.pack();
        client.frame.setVisible(true);
        client.frame.setSize(400, 218);
        client.frame.setResizable(false);
        client.frame.setFocusable(true);
        client.frame.setFocusTraversalKeysEnabled(false);
    }

    public ClientConnection() {
        ipAddressField.setBorder(null);
        portField.setBorder(null);
        usernameField.setBorder(null);

        ipAddressField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (ipAddressField.getText().matches(ipRegex))
                    ipAddressField.setBorder(null);
                else ipAddressField.setBorder(borderError);
            }
        });
        portField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (portField.getText().matches(portRegex))
                    portField.setBorder(null);
                else portField.setBorder(borderError);
            }
        });
        usernameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (usernameField.getText().matches(usernameRegex))
                    usernameField.setBorder(null);
                else usernameField.setBorder(borderError);
            }
        });

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ipAddressField.getText().matches(ipRegex))
                    ipAddressField.setBorder(null);
                else ipAddressField.setBorder(borderError);

                if (portField.getText().matches(portRegex))
                    portField.setBorder(null);
                else portField.setBorder(borderError);

                if (usernameField.getText().matches(usernameRegex))
                    usernameField.setBorder(null);
                else usernameField.setBorder(borderError);

                if (!(ipAddressField.getBorder() == borderError || portField.getBorder() == borderError ||
                usernameField.getBorder() == borderError)) {
                    ip = ipAddressField.getText();
                    port = Integer.parseInt(portField.getText());
                    username = usernameField.getText();

                    try {
                        Socket socket = new Socket(ip, port);

                        BufferedReader serverSend = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String serverReceive = serverSend.readLine();

                        if (serverReceive.equals("Accepted"))
                            openMessenger(socket);
                        else socket.close();

                    } catch (IOException io) { io.printStackTrace(); }
                }
            }
        });
    }

    public void openMessenger(Socket socket) throws IOException {
        ClientMessenger clientMessenger = new ClientMessenger(socket, username);

        frame.remove(panel);
        frame.setSize(700, 500);
        frame.add(clientMessenger.getPanel());
        frame.setVisible(true);
    }
}
