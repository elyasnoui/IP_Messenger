package system;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author Elyas, Noui, elyas.noui@city.ac.uk [190053026]
 * @version Java SE 15
 */

public class ClientHandler implements Runnable {
    // Fields for sending/receiving and storing current clients
    private final Socket client;
    private final BufferedReader in;
    private final PrintWriter out;
    private final ArrayList<ClientHandler> clients;

    // Boolean field used to ensure client's first command is a 'PROTOCOL?' command
    private boolean protocolRequested = false;

    // 'HELP?' dialog
    private final String[] helpDialog = new String[] {
            "[SERVER] Help section - All valid commands",
            "'PROTOCOL? version identifier' where version is an integer > 0 and identifier is a string (Left/Right)",
            "'TIME?' returns the current server time in yyyy/mm/dd hh:mm:ss",
            "'LIST? since headers' since -> unix time (Integers), headers -> Amount of filters you want to add (0-6)",
            "If your headers is over 0, you will need to enter the filters in the format they're stored, i.e:",
            "From: a@a.com\nContents: 2\nTime-taken: 1600000000 etc...",
            "'CANCEL?' if you want to cancel a LIST? request while entering headers",
            "'GET? SHA-256 hash' hash -> SHA-256 hex value which corresponds to unique message",
            "'SAY? message' where message is a string that will broadcast to all peers",
            "'BYE!' leave the server"
    };

    public ClientHandler(Socket client, ArrayList<ClientHandler> clients) throws IOException {
        this.client = client;
        this.clients = clients;

        in = new BufferedReader(new InputStreamReader(client.getInputStream()));   // Used to receive requests
        out = new PrintWriter(client.getOutputStream(), true);            // Used to send responses
    }

    /**
     * This method is a member function of {@link Runnable}, which is used when a {@link Thread} is started.
     * <p>
     *     This method accepts a client and listens for requests.
     *     <br>
     *     Requests are processed and an appropriate response is sent back to the client.
     * </p>
     */
    @Override
    public void run() {
        String request;
        System.out.println("[SERVER] Client joined...");    // Outputs to console that a client has joined.

        // Listens for requests sent by the client, until a break command is reached
        while (true) {
            try {
                request = in.readLine();            // Records the request
                if (request == null) continue;      // Continues looping if the request is null
                if (request.equals("BYE!")) break;  // Breaks the while loop if the request is 'BYE?'

                // Responds to the client with the help dialog and continues to loop if the request is 'HELP?'
                else if (request.equalsIgnoreCase("HELP?")) {
                    for (String s : helpDialog)
                        out.println(s);
                    continue;
                }

                // This checks if the client has sent a 'PROTOCOL?' request yet
                if (!protocolRequested) {
                    // Try/catch is necessary, as sub-stringing requests may generate out of bounds exceptions
                    try {
                        // Request starts with 'PROTOCOL?'
                        if (request.substring(0, 9).equalsIgnoreCase("PROTOCOL?")) {
                            int version = Integer.parseInt(request.substring(10, 11));
                            String identifier;
                            if (request.substring(12).equalsIgnoreCase("LEFT"))
                                identifier = "RIGHT";
                            else identifier = "LEFT";
                            out.println("[SERVER] PROTOCOL? "+version+" "+identifier);
                            protocolRequested = true;
                        }

                        // Request does not start with 'PROTOCOL?', notifies user
                        else out.println("[SERVER] Please input PROTOCOL? command as first request. (HELP? for help)");
                    }

                    // Exception is generated, notify user that the input is invalid
                    catch (StringIndexOutOfBoundsException | NullPointerException ignored) {
                        out.println("[SERVER] Please input PROTOCOL? command as first request. (HELP? for help)");
                    }
                }

                // If a protocol request has already been received, allow for further processing
                else {
                    // 'TIME?' request received, responds with the current date and time
                    if (request.equals("TIME?")) {
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                        LocalDateTime now = LocalDateTime.now();
                        out.println("[SERVER] NOW "+dtf.format(now));
                    }

                    // 'LIST?' request received, process 'since' and 'headers' values
                    else if (request.startsWith("LIST? ")) {
                        // Try/catch required as 'since' and 'headers' may not be formatted correctly
                        try {
                            // Records the 'since' and 'headers' values of the command
                            int fromTime = Integer.parseInt(request.substring(6, 16));
                            int headers = Integer.parseInt(request.substring(17));

                            // Stores all the current messages stored in data directory
                            File dir = new File("data");
                            File[] fileArr = dir.listFiles();
                            List<String> ids = new ArrayList<>();

                            assert fileArr != null;  // Does not continue if the array is null
                            Scanner scanner;         // Used to read each line in text files

                            // User does not want to add any headers to command
                            if (headers == 0) {
                                // Iterates through all files in the directory
                                for (File f : fileArr) {
                                    String filename = "data/"+f.getName();          // Generates path from source
                                    scanner = new Scanner(new File(filename));      // Initiates scanner to read file in path
                                    String id = "";                                 // Initiates field to record message-id

                                    // Iterates through each line in the message
                                    while (scanner.hasNextLine()) {
                                        String line = scanner.nextLine();   // Records the line

                                        // Records the message-id
                                        if (line.startsWith("Message-id: SHA-256 "))
                                            id = line.substring(20);

                                        // Checks if the 'since' parameter is met
                                        if (line.startsWith("Time-sent: ")) {
                                            int timeSent = Integer.parseInt(line.substring(11));   // Records time-sent
                                            if (timeSent > fromTime) ids.add(id);   // Records id if the condition is met
                                        }
                                    }
                                }
                                outputList(ids);  // Passes all ids to outputList, which forwards them to the client
                            }

                            // User wants to add headers to command
                            else {
                                List<String> filters = new ArrayList<>();    // Stores all the headers
                                boolean cancel = false;                      // Helps with cancelling the command

                                // Keeps looping if there still headers that the client needs to input
                                while (headers != 0) {
                                    String filter = in.readLine();  // Reads the header

                                    if (filter == null) continue;   // Continues looping if the header is null

                                    // Cancel the command if the user requests 'CANCEL?'
                                    if (filter.equalsIgnoreCase("CANCEL?")) {
                                        cancel = true;
                                        break;
                                    }

                                    // Adds the header to the filters list
                                    // Headers are already validated before they're sent in ClientMessenger: line 231
                                    else {
                                        filters.add(filter);
                                        headers--;
                                    }
                                }

                                // If the client hasn't cancelled the operation, process the filters
                                if (!cancel) {
                                    // Iterate through each file again, this time reading each line looking to match the filters
                                    for (File f : fileArr) {
                                        String filename = "data/" + f.getName();
                                        scanner = new Scanner(new File(filename));
                                        boolean filtersMatched = true;
                                        String id = "";

                                        while (scanner.hasNextLine()) {
                                            String line = scanner.nextLine();

                                            // Records the id, in case the file matches the filters
                                            if (line.startsWith("Message-id: SHA-256 ")) {
                                                id = line.substring(20);
                                            }

                                            // Declines the file and continues the next if the since condition is not met
                                            if (line.startsWith("Time-sent: ")) {
                                                int timeSent = Integer.parseInt(line.substring(11));
                                                if (timeSent < fromTime) {
                                                    filtersMatched = false;
                                                    break;
                                                }
                                            }

                                            // Compares each filter with the line
                                            // Sets the boolean filtersMatched to true/false depending on matches
                                            for (String filter : filters) {
                                                if (filter.startsWith("Message-id: SHA-256 ") && line.startsWith("Message-id: SHA-256 "))
                                                    filtersMatched = filter.substring(20).equals(line.substring(20));
                                                else if (filter.startsWith("Time-sent: ") && line.startsWith("Time-sent: "))
                                                    filtersMatched = filter.substring(11).equals(line.substring(11));
                                                else if (filter.startsWith("From: ") && line.startsWith("From: "))
                                                    filtersMatched = filter.substring(6).equalsIgnoreCase(line.substring(6));
                                                else if (filter.startsWith("To: ") && line.startsWith("To: "))
                                                    filtersMatched = filter.substring(4).equalsIgnoreCase(line.substring(4));
                                                else if (filter.startsWith("Topic: ") && line.startsWith("Topic: "))
                                                    filtersMatched = filter.substring(7).equals(line.substring(7));
                                                else if (filter.startsWith("Subject: ") && line.startsWith("Subject: "))
                                                    filtersMatched = filter.substring(9).equals(line.substring(9));
                                                else if (filter.startsWith("Contents: ") && line.startsWith("Contents: "))
                                                    filtersMatched = filter.substring(10).equals(line.substring(10));

                                                /*
                                                filterMatched is initialised to 'true'
                                                If at any point filterMatched is set to 'false', one of the filter matches must have failed
                                                Continue to the next file if filterMatched is 'false'
                                                */
                                                if (!filtersMatched) break;
                                            }
                                        }

                                        // No lines have failed the filter matching, add id to the ids list
                                        if (filtersMatched) ids.add(id);
                                    }

                                    outputList(ids);  // Passes all ids to outputList, which forwards them to the client
                                }

                                // Confirm to the client that the 'LIST?' command has been cancelled
                                else out.println("[SERVER] LIST? Command cancelled.");
                            }
                        }

                        // Notify the client that the header input has failed
                        catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {
                            out.println("[SERVER] LIST? Command format error. (HELP? for help)");
                        }
                    }

                    // 'GET?' request received, search for the file and respond found/sorry
                    else if (request.startsWith("GET? SHA-256 ")) {
                        // Records all the messages in the 'data' directory in an array
                        File dir = new File("data");
                        File[] fileArr = dir.listFiles();

                        assert fileArr != null;  // Ensures the file array isn't empty
                        boolean found = false;   // Used to break the loop if the file is found early

                        // Iterates through each file
                        for (File f : fileArr) {
                            // If the file is found
                            if (request.substring(13).equals(f.getName().substring(0, f.getName().length()-4))) {
                                out.println("[SERVER] FOUND");  // Notify to the client that the file has been found
                                String filename = "data/"+request.substring(13)+".txt";  // Generate path to file

                                // Loops through each line in the file and sends it to the client
                                Scanner scanner = new Scanner(new File(filename));
                                StringBuilder output = new StringBuilder();
                                while (scanner.hasNextLine()) {
                                    String line = scanner.nextLine();
                                    output.append(line).append("\n");
                                }
                                out.println(output);

                                // Sets found to 'true' and breaks the loop
                                found = true;
                                break;
                            }
                        }
                        if (!found) out.println("[SERVER] SORRY");  // Notifies client that the file has not been found
                    }

                    // 'SAY?' command received, passes the message to the broadcast method
                    else if (request.startsWith("SAY? "))
                        broadcast(request.substring(5));

                    // 'MSG?' command to identify that the client has sent a full message that must be stored
                    else if (request.startsWith("MSG? ")) {
                        request = request.substring(5);                 // Records the full comma-separated message
                        String[] headers = request.split(",");    // Stores message as array, using the comma to separate elements

                        // Adds all the headers and current unix time to a single string, this will be used to generate the hash id
                        StringBuilder preHashValue = new StringBuilder();
                        long unixTime = System.currentTimeMillis() / 1000L;
                        preHashValue.append(unixTime);
                        for (String s : headers)
                            preHashValue.append(s);

                        String postHashValue = SHA256(preHashValue.toString());  // Generates the hash

                        // Initialises the headers with the format they should be stored in
                        String messageID = "Message-id: SHA-256 " + postHashValue;
                        String time = "Time-sent: " + unixTime;
                        StringBuilder from = new StringBuilder("From: ");
                        StringBuilder to = new StringBuilder("To:");
                        StringBuilder topic = new StringBuilder("Topic:");
                        StringBuilder subject = new StringBuilder("Subject:");
                        StringBuilder lines = new StringBuilder("Contents:");
                        String content = "";

                        // Adds the headers values to the string
                        for (int i=0; i<headers.length; i++) {
                            switch (i) {
                                case 0 -> from.append(headers[i]);
                                case 1 -> to.append(headers[i]);
                                case 2 -> topic.append(headers[i]);
                                case 3 -> subject.append(headers[i]);
                                case 4 -> lines.append(headers[i]);
                                case 5 -> content = headers[i].replaceAll("[\\^]", "\n").substring(1);
                            }
                        }

                        // Creates a file with the name of the hashed id for easier lookup later
                        String fileName = postHashValue+".txt";
                        File fileOut = new File("data/"+fileName);
                        FileOutputStream fileOutputStream = new FileOutputStream(fileOut);
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fileOutputStream));

                        // Creates array storing all the updated fields
                        String fileContent = messageID + '\n' +
                                time + '\n' +
                                from + '\n' +
                                to + '\n' +
                                topic + '\n' +
                                subject + '\n' +
                                lines + '\n' +
                                content + '\n';

                        // Writes the array to the text file and closes
                        bw.write(fileContent);
                        bw.close();

                        // Notifies the client that the message has been recorded
                        out.println("[SERVER] Message received and recorded! ["+messageID+"]");
                    }

                    // Notifies the client that their request is invalid
                    else out.println("[SERVER] Invalid command, enter HELP? for guidance.");
                }
            }

            // Catches any socket errors and prints the error trace
            catch (IOException e) { e.printStackTrace(); }
        }

        // Closes the socket and terminate the client
        try {
            out.println("BYE?");  // Used to exit the loop in ServerHandler: line 47
            in.close();
            out.close();
            client.close();
            clients.remove(this);

            // Terminates the server if there are no more clients
            if (clients.size() == 0) System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Records all IDs stored in the list and outputs them to the client.
     * <p>
     *     This is used when a 'LIST?' command is invoked and the server has finished searching for all the messages.
     * </p>
     * @param ids {@link List<String>} array containing all message-ids.
     */
    private void outputList(List<String> ids) {
        if (!ids.isEmpty()) {
            StringBuilder output = new StringBuilder("[SERVER] MESSAGES " + ids.size() + "\n");
            for (int i = 0; i < ids.size(); i++) {
                if (i != ids.size()-1)
                    output.append(ids.get(i)).append("\n");
                else output.append(ids.get(i));
            }
            out.println(output);
        } else out.println("[SERVER] SORRY");
    }

    /**
     * Takes in a string, hashes the string using the SHA-256 algorithm and returns the hashed value.
     * <p>
     *     This is used when generated message-ids for messages.
     * </p>
     * @param preHash {@link String} value.
     * @return {@link String} hexValue (SHA-256 Hash).
     */
    private String SHA256(final String preHash) {
        try{
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(preHash.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                final String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    /**
     * This method is used to broadcast a message to all clients connected to the server.
     * <p>
     *     This is used as a form of quick chat between connected clients.
     * </p>
     * @param message {@link String}
     */
    private void broadcast(String message) {
        for (ClientHandler c : clients)
            c.out.println(message);
    }
}