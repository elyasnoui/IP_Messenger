package system;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * @author Elyas, Noui, elyas.noui@city.ac.uk [190053026]
 * @version Java SE 15
 */

public class ClientConnection {
    // GUI fields
    private JTextField ipAddressField, portField, usernameField;
    private JButton clientButton;
    private JPanel panel;
    private JFrame frame;

    // Socket parameter fields and username
    private String username, ip;
    private int port;

    // Regex Validation fields
    private final String ipRegex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private final String portRegex = "[0-9]{1,5}";
    private final String usernameRegex = ".{1,10}";
    private final LineBorder borderError = new LineBorder(Color.RED, 1);

    public static void main(String[] args) {
        ClientConnection client = new ClientConnection();

        // Initialising the JFrame and setting parameters
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

    // Constructor is private, as this should only be instantiated from the class' 'main' method
    private ClientConnection() {
        // Cosmetic
        ipAddressField.setBorder(null);
        portField.setBorder(null);
        usernameField.setBorder(null);

        // IP address validator
        ipAddressField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (ipAddressField.getText().matches(ipRegex))
                    ipAddressField.setBorder(null);
                else ipAddressField.setBorder(borderError);
            }
        });
        // Port number validator
        portField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (portField.getText().matches(portRegex))
                    portField.setBorder(null);
                else portField.setBorder(borderError);
            }
        });
        // Username validator
        usernameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (usernameField.getText().matches(usernameRegex))
                    usernameField.setBorder(null);
                else usernameField.setBorder(borderError);
            }
        });

        // When pressing the 'Start Client' button
        clientButton.addActionListener(e -> {
            // Validating all fields
            if (ipAddressField.getText().matches(ipRegex))
                ipAddressField.setBorder(null);
            else ipAddressField.setBorder(borderError);

            if (portField.getText().matches(portRegex))
                portField.setBorder(null);
            else portField.setBorder(borderError);

            if (usernameField.getText().matches(usernameRegex))
                usernameField.setBorder(null);
            else usernameField.setBorder(borderError);

            // Checks if all fields are valid
            if (!(ipAddressField.getBorder() == borderError || portField.getBorder() == borderError ||
            usernameField.getBorder() == borderError)) {
                // Record the fields
                ip = ipAddressField.getText();
                port = Integer.parseInt(portField.getText());
                username = usernameField.getText();

                try {
                    // Connect to server using ip and port recorded from text fields
                    Socket socket = new Socket(ip, port);

                    BufferedReader serverSend = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String serverReceive = serverSend.readLine();

                    // Checking if a connection has been established
                    if (serverReceive.equals("Accepted"))
                        openMessenger(socket); // Open 'Client Messenger' window
                    else socket.close(); // Close socket if there's no connection

                }
                // Catch and print source of any IOException errors
                catch (IOException io) {
                    io.printStackTrace();
                }
            }
        });

        // Same process as pressing the 'Start Client' button, initiates if user hits Enter on any field
        KeyAdapter listener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
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
            }
        };
        ipAddressField.addKeyListener(listener);
        portField.addKeyListener(listener);
        usernameField.addKeyListener(listener);
    }

    /**
     * This method opens the client messenger by instantiating {@link ClientMessenger}.
     * <p>
     *     This is used when a connection has been established from a client to a server.
     * </p>
     * @param socket {@link Socket}
     * @throws IOException {@link IOException} gets ignored.
     */
    private void openMessenger(Socket socket) throws IOException {
        // Creating new Client Messenger object and passing in socket and username
        ClientMessenger clientMessenger = new ClientMessenger(socket, username);

        // Switch panels to Client Messenger
        frame.remove(panel);
        frame.setSize(1280, 720);
        frame.add(clientMessenger.getPanel());
        frame.setVisible(true);
    }
}
