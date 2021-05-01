package system;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

/**
 * @author Elyas, Noui, elyas.noui@city.ac.uk [190053026]
 * @version Java SE 15
 */

public class ServerHandler implements Runnable {
    private final BufferedReader in;
    private final JList<Object> messageList;
    private final ArrayList<String> chatLog;
    private final JScrollPane scrollPane;

    public ServerHandler(Socket server, JList<Object> messageList, ArrayList<String> chatLog, JScrollPane scrollPane) throws IOException {
        this.messageList = messageList;
        this.chatLog = chatLog;
        this.scrollPane = scrollPane;

        // Used to receive responses from the server
        in = new BufferedReader(new InputStreamReader(server.getInputStream()));

        // Bug fix: GUI JList would sometimes go blank and would need a reset, this keeps the list refreshing
        Timer timer = new Timer(500, arg0 -> messageList.setListData(chatLog.toArray()));
        timer.start();
    }

    /**
     * This method is a member function of {@link Runnable}, which is used when a {@link Thread} is started.
     * <p>
     *     This method allows for server responses for clients to be displayed on the client's GUI.
     * </p>
     */
    @Override
    public void run() {
        try {
            String response;
            while (true) {
                response = in.readLine();            // Records the response
                if (response == null) continue;      // Keep looping if the response is null
                if (response.equals("BYE?")) break;  // Used to escape infinite loop

                // Displays the response on the client's JList
                chatLog.add(response);
                messageList.setListData(chatLog.toArray());
                scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum()+1);
            }
        } catch (IOException e) { e.printStackTrace(); }
        finally {
            try { in.close(); }
            catch (IOException e) { e.printStackTrace(); }
        }
    }
}
