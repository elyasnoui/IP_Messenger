package client;

import server.ServerHandler;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ClientMessenger {
    private JPanel panel;
    private JButton requestButton;
    private JList messageList;
    private JTextField requestField;
    private JButton exitButton;
    private JTextField fromField;
    private JTextArea contentField;
    private JTextField toField;
    private JTextField topicField;
    private JTextField subjectField;
    private JButton sendButton;
    private JScrollPane scrollPane;
    private ArrayList<String> chatLog = new ArrayList<>();

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private static final String emailRegex = "([A-Za-z0-9_\\-\\.])+\\@([A-Za-z0-9_\\-\\.])+\\.([A-Za-z]{2,4})";
    private static final String nameRegex = "[A-Z]{1}[a-zA-z-]{1,34}";
    private static final String topicRegex = "[#][A-Za-z0-9]{1,20}";
    private static final String subjectRegex = "[A-Za-z0-9 ?!._,-]{1,20}";
    private static final String contentRegex = "[A-Za-z0-9\s\n?!\"._,-]{1,75}";
    private static final LineBorder borderError = new LineBorder(Color.RED, 1);

    public ClientMessenger(Socket socket, String username) throws IOException {
        this.socket = socket;
        out = new PrintWriter(socket.getOutputStream(), true);
        //in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ServerHandler serverHandler = new ServerHandler(socket, messageList, chatLog, scrollPane);

        new Thread(serverHandler).start();

        requestField.setBorder(null);
        fromField.setBorder(null);
        contentField.setBorder(null);
        toField.setBorder(null);
        topicField.setBorder(null);
        subjectField.setBorder(null);

        requestField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    requestToServer(username);
            }
        });
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                out.println("BYE!");
                System.exit(0);
            }
        });
        requestButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                requestToServer(username);
            }
        });

        //Regex Validation
        fromField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (fromField.getText().matches(emailRegex) || fromField.getText().matches(nameRegex))
                    fromField.setBorder(null);
                else fromField.setBorder(borderError);
            }
        });
        toField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (toField.getText().matches(emailRegex) || toField.getText().matches(nameRegex))
                    toField.setBorder(null);
                else toField.setBorder(borderError);
            }
        });
        topicField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (topicField.getText().matches(topicRegex))
                    topicField.setBorder(null);
                else topicField.setBorder(borderError);
            }
        });
        subjectField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (subjectField.getText().matches(subjectRegex))
                    subjectField.setBorder(null);
                else subjectField.setBorder(borderError);
            }
        });
        contentField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (contentField.getText().matches(contentRegex))
                    contentField.setBorder(null);
                else contentField.setBorder(borderError);
            }
        });

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fromField.getText().matches(emailRegex) || fromField.getText().matches(nameRegex))
                    fromField.setBorder(null);
                else fromField.setBorder(borderError);

                if (toField.getText().matches(emailRegex) || toField.getText().matches(nameRegex))
                    toField.setBorder(null);
                else toField.setBorder(borderError);

                if (topicField.getText().matches(topicRegex))
                    topicField.setBorder(null);
                else topicField.setBorder(borderError);

                if (subjectField.getText().matches(subjectRegex))
                    subjectField.setBorder(null);
                else subjectField.setBorder(borderError);

                if (contentField.getText().matches(contentRegex))
                    contentField.setBorder(null);
                else contentField.setBorder(borderError);

                if (!(fromField.getBorder() == borderError || toField.getBorder() == borderError ||
                topicField.getBorder() == borderError || subjectField.getBorder() == borderError ||
                contentField.getBorder() == borderError))
                {
                    String[] lines = contentField.getText().split("\n");
                    int lineCount = lines.length;

                    String content = contentField.getText().replaceAll("[\r\n]+", "^");

                    String[] message = {
                            fromField.getText(),
                            toField.getText(),
                            topicField.getText(),
                            subjectField.getText(),
                            String.valueOf(lineCount),
                            content
                    };

                    String messageStream = "";
                    for (String s : message)
                        messageStream += s + ", ";
                    messageStream = messageStream.substring(0, messageStream.length()-2);

                    out.println("MSG? "+messageStream);

                    fromField.setText("");
                    toField.setText("");
                    topicField.setText("");
                    subjectField.setText("");
                    contentField.setText("");
                }
            }
        });
    }

    private void requestToServer(String username) {
        if (requestField.getText().startsWith("PROTOCOL?")) {
            try {
                if (Integer.parseInt(requestField.getText().substring(10, 11)) > 0 &&
                        (requestField.getText().substring(12).equalsIgnoreCase("LEFT") ||
                                requestField.getText().substring(12).equalsIgnoreCase("RIGHT"))) {

                    out.println(requestField.getText());

                } else JOptionPane.showMessageDialog(panel, "PROTOCOL? Request format invalid.");
            } catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {}
        } else if (requestField.getText().equals("TIME?"))
            out.println(requestField.getText());
        else if (requestField.getText().startsWith("SAY? "))
            out.println("SAY? "+username+": "+ requestField.getText().substring(5));
        else if (requestField.getText().equalsIgnoreCase("BYE!")) {
            out.println("BYE!");
            System.exit(0);
        }
        else out.println(requestField.getText());

        requestField.setText("");
    }

    public JPanel getPanel() {
        return panel;
    }
}
