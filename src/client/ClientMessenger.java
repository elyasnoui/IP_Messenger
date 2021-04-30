package client;

import server.ServerHandler;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
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
    private int headers = 0;
    private String username;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private static final String hashRegex = "[A-Za-z0-9]{64}";
    private static final String unixTimeRegex = "[0-9]{10}";
    private static final String emailRegex = "([A-Za-z0-9_\\-\\.])+\\@([A-Za-z0-9_\\-\\.])+\\.([A-Za-z]{2,4})";
    private static final String nameRegex = "[A-Z][a-zA-z-]{1,34}";
    private static final String topicRegex = "[#][A-Za-z0-9]{1,20}";
    private static final String subjectRegex = "[A-Za-z0-9 ?!._,-]{1,20}";
    private static final String contentRegex = "[A-Za-z0-9\s\n?!\"._,-]{1,75}";
    private static final LineBorder borderError = new LineBorder(Color.RED, 1);

    public ClientMessenger(Socket socket, String username) throws IOException {
        this.socket = socket;
        this.username = username;
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

        chatLog.add("Welcome to the client messenger! Please use a PROTOCOL? command first, enter HELP? for guidance.");

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
        MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (e.getComponent().getBackground().equals(new Color(76, 84, 118)))
                    e.getComponent().setBackground(new Color(176, 191, 241));
                else if (e.getComponent().getBackground().equals(new Color(175, 76, 65)))
                    e.getComponent().setBackground(new Color(255, 114, 99));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (e.getComponent().getBackground().equals(new Color(176, 191, 241)))
                    e.getComponent().setBackground(new Color(76, 84, 118));
                else if (e.getComponent().getBackground().equals(new Color(255, 114, 99)))
                    e.getComponent().setBackground(new Color(175, 76, 65));
            }
        };
        requestButton.addMouseListener(listener);
        exitButton.addMouseListener(listener);
        sendButton.addMouseListener(listener);
    }

    private void requestToServer(String username) {
        if (headers > 0) {
            String request = requestField.getText();
            boolean verified = false;

            try {
                if (request.startsWith("Message-id: "))
                    verified = request.substring(12).matches(hashRegex);
                else if (request.startsWith("Time-sent: "))
                    verified = request.substring(11).matches(unixTimeRegex);
                else if (request.startsWith("From: "))
                    verified = request.substring(6).matches(emailRegex) || request.substring(6).matches(nameRegex);
                else if (request.startsWith("To: "))
                    verified = request.substring(4).matches(emailRegex) || request.substring(4).matches(nameRegex);
                else if (request.startsWith("Topic: "))
                    verified = request.substring(7).matches(topicRegex);
                else if (request.startsWith("Subject: "))
                    verified = request.substring(9).matches(subjectRegex);
                else if (request.startsWith("Contents: "))
                    verified = request.substring(10).matches(contentRegex);
                else if (request.equalsIgnoreCase("CANCEL?")) {
                    headers = 0;
                    out.println(request);
                    updateLog(request);
                }

                if (verified) {
                    out.println(request);
                    updateLog(request);
                    headers--;
                } else if (!request.equalsIgnoreCase("CANCEL?"))
                    JOptionPane.showMessageDialog(panel, "LIST? Filter format error. (HELP? for help)");
            } catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {
                JOptionPane.showMessageDialog(panel, "LIST? Filter format invalid. (HELP? for help)");
            }

        } else {
            if (requestField.getText().startsWith("PROTOCOL?")) {
                try {
                    if (Integer.parseInt(requestField.getText().substring(10, 11)) > 0 &&
                            (requestField.getText().substring(12).equalsIgnoreCase("LEFT") ||
                                    requestField.getText().substring(12).equalsIgnoreCase("RIGHT"))) {

                        out.println(requestField.getText());
                        updateLog(requestField.getText());

                    } else JOptionPane.showMessageDialog(panel, "PROTOCOL? Request format invalid. (HELP? for help)");
                } catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {
                    JOptionPane.showMessageDialog(panel, "PROTOCOL? Request format invalid. (HELP? for help)");
                }
            }
            else if (requestField.getText().equalsIgnoreCase("TIME?")) {
                out.println(requestField.getText());
                updateLog("TIME?");
            }
            else if (requestField.getText().startsWith("LIST? ")) {
                try {
                    headers = Integer.parseInt(requestField.getText().substring(17));

                    if (headers > -1 && headers < 7 && requestField.getText().charAt(16) == ' ') {
                        out.println(requestField.getText());
                        updateLog(requestField.getText());
                    }
                    else JOptionPane.showMessageDialog(panel, "LIST? Request format error. (HELP? for help)");
                } catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {
                    JOptionPane.showMessageDialog(panel, "LIST? Request format invalid. (HELP? for help)");
                }
            }
            else if (requestField.getText().startsWith("SAY? "))
                out.println("SAY? "+username+": "+ requestField.getText().substring(5));
            else if (requestField.getText().equalsIgnoreCase("BYE!")) {
                out.println("BYE!");
                System.exit(0);
            }
            else out.println(requestField.getText());
        }

        requestField.setText("");
    }

    private void updateLog(String request) {
        chatLog.add("["+username+"] " + request);
        messageList.setListData(chatLog.toArray());
        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum()+1);
    }

    public JPanel getPanel() {
        return panel;
    }
}
