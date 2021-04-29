package client;

import javax.crypto.AEADBadTagException;
import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ClientHandler implements Runnable {
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private ArrayList<ClientHandler> clients;
    private boolean protocolRequested = false;

    private String[] helpDialog = new String[] {
            "[SERVER] Help section - All valid commands",
            "'PROTOCOL? version identifier' where version is an integer > 0 and identifier is a string (Left/Right)",
            "'TIME?' returns the current server time in yyyy/mm/dd hh:mm:ss",
            "'LIST? since headers' time -> integers, headers -> integer where headers > 0",
            "'GET? SHA-256 hash' hash -> hex value which corresponds to unique message",
            "'SAY? message' where message is a string that will broadcast to all peers",
            "'BYE!' leave the server"
    };

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
        while (true) {
            try {
                message = in.readLine();

                if (message == null) continue;

                if (message.equals("BYE!")) break;
                else if (message.equalsIgnoreCase("HELP?")) {
                    for (String s : helpDialog)
                        out.println(s);
                    continue;
                }

                if (!protocolRequested) {
                    try {
                        if (message.substring(0, 9).equalsIgnoreCase("PROTOCOL?")) {
                            int version = Integer.parseInt(message.substring(10, 11));
                            String identifier;
                            if (message.substring(12).equalsIgnoreCase("LEFT"))
                                identifier = "RIGHT";
                            else identifier = "LEFT";
                            out.println("[SERVER] PROTOCOL? "+version+" "+identifier);
                            protocolRequested = true;
                        } else {
                            out.println("[SERVER] Please input PROTOCOL? command as first request. (HELP? for help)");
                        }
                    } catch (StringIndexOutOfBoundsException ignored) {
                        out.println("[SERVER] Please input PROTOCOL? command as first request. (HELP? for help)");
                    }
                } else {
                    if (message.equals("TIME?")) {
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                        LocalDateTime now = LocalDateTime.now();
                        out.println("[SERVER] NOW "+dtf.format(now));
                    }
                    else if (message.startsWith("LIST? ")) {
                        try {
                            int fromTime = Integer.parseInt(message.substring(6, 16));
                            int headers = Integer.parseInt(message.substring(17));

                            File dir = new File("data");
                            File[] fileArr = dir.listFiles();
                            assert fileArr != null;
                            Scanner scanner;

                            if (headers == 0) {
                                dir = new File("data");
                                fileArr = dir.listFiles();
                                assert fileArr != null;
                                List<String> ids = new ArrayList<>();
                                for (File f : fileArr) {
                                    String filename = "data/"+f.getName();
                                    scanner = new Scanner(new File(filename));

                                    while (scanner.hasNextLine()) {
                                        String line = scanner.nextLine();
                                        String id = "";

                                        if (line.startsWith("Message-id: SHA-256 "))
                                            id = line.substring(20);

                                        if (line.startsWith("Time-sent: ")) {
                                            int timeSent = Integer.parseInt(line.substring(11));
                                            if (timeSent > fromTime) ids.add(id);
                                        }
                                    }
                                }

                                if (!ids.isEmpty()) {
                                    out.println("[SERVER] MESSAGES " + ids.size());

                                    for (String id : ids)
                                        out.println(id);
                                }
                            } else {
                                List<String> filters = new ArrayList<>();
                                boolean cancel = false;

                                while (headers != 0) {
                                    String filter = in.readLine();

                                    if (filter == null) continue;

                                    if (filter.equalsIgnoreCase("CANCEL?")) {
                                        cancel = true;
                                        break;
                                    }
                                    else {
                                        filters.add(filter);
                                        headers--;
                                    }
                                }

                                if (!cancel) {
                                    List<String> ids = new ArrayList<>();
                                    for (File f : fileArr) {
                                        String filename = "data/"+f.getName();
                                        scanner = new Scanner(new File(filename));
                                        boolean filtersMatched = true;

                                        while (scanner.hasNextLine()) {
                                            String line = scanner.nextLine();
                                            String id = "";

                                            if (line.startsWith("Message-id: SHA-256 "))
                                                id = line.substring(20);

                                            for (String filter : filters) {

                                                if (filter.startsWith("Message-id: SHA-256 ") && line.startsWith("Message-id: SHA-256 "))
                                                    filtersMatched = filter.substring(20).equalsIgnoreCase(line.substring(20));
                                                else if (filter.startsWith("Time-sent: ") && line.startsWith("Time-sent: "))
                                                    filtersMatched = filter.substring(11).equalsIgnoreCase(line.substring(11));
                                                else if (filter.startsWith("From: ") && line.startsWith("From: "))
                                                    filtersMatched = filter.substring(6).equalsIgnoreCase(line.substring(6));
                                                else if (filter.startsWith("To: ") && line.startsWith("To: "))
                                                    filtersMatched = filter.substring(4).equalsIgnoreCase(line.substring(4));
                                                else if (filter.startsWith("Topic: ") && line.startsWith("Topic: "))
                                                    filtersMatched = filter.substring(7).equalsIgnoreCase(line.substring(7));
                                                else if (filter.startsWith("Subject: ") && line.startsWith("Subject: "))
                                                    filtersMatched = filter.substring(9).equalsIgnoreCase(line.substring(9));
                                                else if (filter.startsWith("Contents: ") && line.startsWith("Contents: "))
                                                    filtersMatched = filter.substring(10).equalsIgnoreCase(line.substring(10));

                                                if (!filtersMatched) break;
                                            }

                                            if (!filtersMatched) break;
                                            else ids.add(id);
                                        }
                                    }

                                    out.println("[SERVER] MESSAGES " + ids.size());
                                    if (!ids.isEmpty())
                                        for (String id : ids)
                                            out.println(id);

                                } else out.println("[SERVER] LIST? Command cancelled.");
                            }

                        } catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {
                            out.println("[SERVER] LIST? Command format error. (HELP? for help)");
                        }
                    }
                    else if (message.startsWith("GET? SHA-256 ")) {
                        File dir = new File("data");
                        File[] fileArr = dir.listFiles();
                        assert fileArr != null;
                        boolean found = false;
                        for (File f : fileArr) {
                            if (message.substring(13).equals(f.getName().substring(0, f.getName().length()-4))) {
                                out.println("[SERVER] FOUND");
                                String filename = "data/"+message.substring(13)+".txt";

                                Scanner scanner = new Scanner(new File(filename));
                                String output = "";
                                while (scanner.hasNextLine()) {
                                    String line = scanner.nextLine();
                                    output += line + "\n";
                                }
                                out.println(output);

                                found = true;
                                break;
                            }
                        }

                        if (!found) out.println("[SERVER] SORRY");
                    }
                    else if (message.startsWith("SAY? "))
                        broadcast(message.substring(5));
                    else if (message.startsWith("MSG? ")) {
                        message = message.substring(5);
                        String[] headers = message.split(",");

                        String preHashValue = "";
                        long unixTime = System.currentTimeMillis() / 1000L;
                        preHashValue += unixTime;
                        for (String s : headers)
                            preHashValue += s;

                        String messageID = "Message-id: SHA-256 "+SHA256(preHashValue);
                        String time = "Time-sent: "+unixTime;
                        String from = "From: ";
                        String to = "To:";
                        String topic = "Topic:";
                        String subject = "Subject:";
                        String lines = "Contents:";
                        String content = "";
                        for (int i=0; i<headers.length; i++) {
                            switch (i) {
                                case 0:
                                    from += headers[i];
                                    break;
                                case 1:
                                    to += headers[i];
                                    break;
                                case 2:
                                    topic += headers[i];
                                    break;
                                case 3:
                                    subject += headers[i];
                                    break;
                                case 4:
                                    lines += headers[i];
                                    break;
                                case 5:
                                    content = headers[i].replaceAll("[\\^]", "\n").substring(1);
                                    break;
                            }
                        }

                        String fileName = SHA256(preHashValue)+".txt";
                        File fileOut = new File("data/"+fileName);
                        FileOutputStream fileOutputStream = new FileOutputStream(fileOut);

                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fileOutputStream));

                        StringBuilder fileContent = new StringBuilder();

                        fileContent.append(messageID).append('\n');
                        fileContent.append(time).append('\n');
                        fileContent.append(from).append('\n');
                        fileContent.append(to).append('\n');
                        fileContent.append(topic).append('\n');
                        fileContent.append(subject).append('\n');
                        fileContent.append(lines).append('\n');
                        fileContent.append(content).append('\n');

                        bw.write(String.valueOf(fileContent));
                        bw.close();

                        out.println("[SERVER] Message received and recorded! ["+messageID+"]");
                    }
                    else out.println("[SERVER] Invalid command, enter HELP? for guidance.");
                }

            } catch (IOException e) { e.printStackTrace(); }
        }
        try {
            in.close();
            out.close();
            client.close();
            clients.remove(this);
            if (clients.size() == 0) System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String SHA256(final String preHash) {
        try{
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(preHash.getBytes("UTF-8"));
            final StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                final String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

    private void broadcast(String message) {
        for (ClientHandler c : clients)
            c.out.println(message);
    }
}