package system;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

/**
 * @author Elyas, Noui, elyas.noui@city.ac.uk [190053026]
 * @version Java SE 15
 */

public class ClientMessenger {
    // GUI fields
    private JPanel panel;
    private JButton requestButton;
    private JList<Object> messageList;
    private JTextField requestField;
    private JButton exitButton;
    private JTextField fromField;
    private JTextArea contentField;
    private JTextField toField;
    private JTextField topicField;
    private JTextField subjectField;
    private JButton sendButton;
    private JScrollPane scrollPane;
    private final ArrayList<String> chatLog = new ArrayList<>();

    // Field for sending messages to the server
    private final String username;
    private final PrintWriter out;
    private int headers = 0;

    // Regex values
    private static final String hashRegex = "[A-Za-z0-9]{64}";
    private static final String unixTimeRegex = "[0-9]{10}";
    private static final String emailRegex = "([A-Za-z0-9_\\-.])+@([A-Za-z0-9_\\-.])+\\.([A-Za-z]{2,4})";
    private static final String nameRegex = "[A-Z][a-zA-z-]{1,34}";
    private static final String topicRegex = "[#][A-Za-z0-9]{1,20}";
    private static final String subjectRegex = "[A-Za-z0-9 ?!._,-]{1,20}";
    private static final String contentRegex = "[A-Za-z0-9\s\n?!\"._,-]{1,75}";
    private static final LineBorder borderError = new LineBorder(Color.RED, 1);

    public ClientMessenger(Socket socket, String username) throws IOException {
        this.username = username;
        // This PrintWriter is used to send requests to the server
        out = new PrintWriter(socket.getOutputStream(), true);
        // This class is used to receive responses from the server
        ServerHandler serverHandler = new ServerHandler(socket, messageList, chatLog, scrollPane);

        // Thread allowing for multiple client communication to/from the server
        new Thread(serverHandler).start();

        // Cosmetics
        requestField.setBorder(null);
        fromField.setBorder(null);
        contentField.setBorder(null);
        toField.setBorder(null);
        topicField.setBorder(null);
        subjectField.setBorder(null);

        // Initial message to client
        chatLog.add("Welcome to the client messenger! Please use a PROTOCOL? command first, enter HELP? for guidance.");

        // Send the request if 'Enter' is hit on the request line
        requestField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    requestToServer(username);
            }
        });
        // Send 'BYE!' request and terminate the program if the 'Close' button is pressed
        exitButton.addActionListener(e -> {
            out.println("BYE!");
            System.exit(0);
        });
        // Send the request command if the 'Request' button is hit
        requestButton.addActionListener(e -> requestToServer(username));

        //Regex validation for fields in Message section
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

        // When pressing the 'Send' button for messages
        sendButton.addActionListener(e -> {
            // Validating all fields
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

            // Checks if all fields are valid
            if (!(fromField.getBorder() == borderError || toField.getBorder() == borderError ||
            topicField.getBorder() == borderError || subjectField.getBorder() == borderError ||
            contentField.getBorder() == borderError))
            {
                // Counting the contents of the message
                String[] lines = contentField.getText().split("\n");
                int lineCount = lines.length;

                // Restructuring the content to fit in one line
                String content = contentField.getText().replaceAll("[\r\n]+", "^");

                // Storing all fields in an array for easy iteration
                String[] message = {
                        fromField.getText(),
                        toField.getText(),
                        topicField.getText(),
                        subjectField.getText(),
                        String.valueOf(lineCount),
                        content
                };

                // Adding all fields into one line, separated by commas
                StringBuilder messageStream = new StringBuilder();
                for (String s : message)
                    messageStream.append(s).append(", ");
                messageStream = new StringBuilder(messageStream.substring(0, messageStream.length() - 2));

                // Sending the message using the 'MSG?' command, this helps the server identify it's a message
                out.println("MSG? "+messageStream);

                // Resetting field values
                fromField.setText("");
                toField.setText("");
                topicField.setText("");
                subjectField.setText("");
                contentField.setText("");
            }
        });
        // This listener allows for highlight effect when hovering over buttons
        MouseAdapter listener = new MouseAdapter() {
            // When mouse is hovering
            @Override
            public void mouseEntered(MouseEvent e) {
                if (e.getComponent().getBackground().equals(new Color(76, 84, 118)))
                    e.getComponent().setBackground(new Color(176, 191, 241));
                else if (e.getComponent().getBackground().equals(new Color(175, 76, 65)))
                    e.getComponent().setBackground(new Color(255, 114, 99));
            }

            // When mouse has exited
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

    /**
     * This method validates the user's request before sending it to the Server, declining any incorrect statements.
     * <p>
     *     This method is used when {@link ClientMessenger#requestButton} is pressed or Enter is hit on {@link ClientMessenger#requestField}.
     *     <br>
     *     {@link ClientMessenger#username} is used to help the server identify the client sending the requests.
     * </p>
     * @param username String value.
     */
    private void requestToServer(String username) {
        // This helps to check whether a 'LIST?' command that has multiple headers is still active
        if (headers > 0) {
            String request = requestField.getText();    // Header field inputted by user
            boolean verified = false;                   // Used to verify whether the header has been inputted correctly

            // Filtering through the header may invoke an exception, so it must use a try/catch
            try {
                // Checks what kind of filter it is and uses regex to validate it
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

                // Cancels the 'List?' request if the user no longer wants to finish the command
                else if (request.equalsIgnoreCase("CANCEL?")) {
                    headers = 0;
                    out.println(request);
                    updateLog(request);
                }

                // Sends header and updates GUI if the input is valid
                if (verified) {
                    out.println(request);
                    updateLog(request);
                    headers--;
                }

                // Displays a dialog box if the header input is almost valid nor is a 'CANCEL?' command
                else if (!request.equalsIgnoreCase("CANCEL?"))
                    JOptionPane.showMessageDialog(panel, "LIST? Filter format error. (HELP? for help)");
            }

            // Displays a dialog box if the header input isn't valid at all
            catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {
                JOptionPane.showMessageDialog(panel, "LIST? Filter format invalid. (HELP? for help)");
            }
        }

        // This means that the 'LIST?' command isn't active, so the input is a request
        else {
            // User has inputted a 'PROTOCOL?' command
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

            // User has inputted a 'TIME?' command
            else if (requestField.getText().equalsIgnoreCase("TIME?")) {
                out.println(requestField.getText());
                updateLog("TIME?");
            }

            // User has inputted a 'LIST?' command
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

            // User has inputted a 'SAY?' command
            else if (requestField.getText().startsWith("SAY? "))
                out.println("SAY? "+username+": "+ requestField.getText().substring(5));

            // User has inputted a 'BYE!' command
            else if (requestField.getText().equalsIgnoreCase("BYE!")) {
                out.println("BYE!");
                System.exit(0);
            }

            // Disallows user to send a full message using the request line
            else if (requestField.getText().startsWith("MSG? "))
                JOptionPane.showMessageDialog(panel, "Please send a message using the form below.");

            // User has inputted a random string, this will likely be denied by the server
            else out.println(requestField.getText());
        }

        // Resets the request text field
        requestField.setText("");
    }

    /**
     * Adds the request to {@link ClientMessenger#chatLog} and displays it on the GUI ({@link ClientMessenger#messageList}).
     * <p>
     *     This is used when a Client successfully makes a request command to the Server.
     * </p>
     * @param request String value.
     */
    private void updateLog(String request) {
        // Attaches the username to the request and adds to the chat log
        chatLog.add("["+username+"] " + request);
        // Displays chatLog on the JList, which can be seen in the GUI
        messageList.setListData(chatLog.toArray());
        // Scrolls the bar to the bottom
        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum()+1);
    }

    /**
     * Returns the panel used to display the GUI components.
     * <p>
     *     This is used to switch the window from {@link ClientConnection} to {@link ClientHandler}.
     * </p>
     * @return {@link ClientMessenger#panel}
     */
    public JPanel getPanel() {
        return panel;
    }
}
