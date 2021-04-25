package client;

import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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
                    } else if (message.startsWith("GET? SHA-256 ")) {
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
                        String from = "From:";
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
