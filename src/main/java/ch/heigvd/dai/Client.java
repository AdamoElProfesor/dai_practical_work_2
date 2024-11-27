package ch.heigvd.dai;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;


class Client {
    private static final String HOST = "localhost";
    private static final int PORT = 1234;
    public static String END_OF_LINE = "\n";

    public static void main(String[] args) {
        System.out.println("[Client] Connecting to " + HOST + ":" + PORT + "...");

        try (Socket socket = new Socket(HOST, PORT);

             Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(reader);

             Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
             BufferedWriter out = new BufferedWriter(writer); ) {

            System.out.println("[Client] Connected to " + HOST + ":" + PORT);

            //Process server responses
            Thread serverListener = new Thread(new ServerListener(socket));
            serverListener.start();

            while (!socket.isClosed()) {
                System.out.print("> ");

                // Read user input
                Reader inputReader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
                BufferedReader bir = new BufferedReader(inputReader);

                String userInput = bir.readLine(); // blocking
                processClientInput(userInput, out);

            }
            System.out.println("[Client] Closing connection and quitting...");
        } catch (IOException e) {
            System.out.println("[Client] IO exception: " + e);
        }
    }

    // Returns true if no errors were found
    // Returns false if the client input was invalid / unknown
    private static boolean processClientInput(String input, BufferedWriter out) throws IOException {

        String[] userInputParts = input.split(" ", 2);
        ClientCommand command = null;
        try{
            command = ClientCommand.valueOf(userInputParts[0].toUpperCase()); // Throws exception if no match
        } catch (Exception e){
            System.out.println("[Client] Invalid command: " + input);
            return false;
        }

        String request = null;

        switch (command){
            case JOIN:
                String name = userInputParts[1];
                request = ClientCommand.JOIN + " " + name;
                break;
            case SEND_PRIVATE:
                String[] splitRecipientAndMessage = userInputParts[1].split(" ");
                if(splitRecipientAndMessage.length < 2){
                    System.out.println("[Client] Invalid recipient: " + splitRecipientAndMessage.length);
                    return false;
                }
                String recipient = userInputParts[1].split(" ", 2)[0];
                String content = userInputParts[1].split(" ", 2)[1];

                request = ClientCommand.SEND_PRIVATE + " " + recipient + " " + content;
                break;
            case SEND_GROUP:
                String[] splitGroupAndMessage = userInputParts[1].split(" ");
                if(splitGroupAndMessage.length < 2){
                    System.out.println("[Client] Invalid recipient: " + splitGroupAndMessage.length);
                    return false;
                }
                String group = userInputParts[1].split(" ", 2)[0];
                String message = userInputParts[1].split(" ", 2)[1];

                request = ClientCommand.SEND_GROUP + " " + group.toUpperCase() + " " + message;
                break;
            case PARTICIPATE:
                String groupName = userInputParts[1];
                request = ClientCommand.PARTICIPATE + " " + groupName.toUpperCase();
                break;
            case HISTORY:
                String groupNameHistory = userInputParts[1];
                request = ClientCommand.HISTORY + " " + groupNameHistory.toUpperCase();
                break;
            case LIST_GROUPS:
                request = ClientCommand.LIST_GROUPS + "";
                break;
        }

        //Useless condition for the moment
        if (request != null) {
            out.write(request + END_OF_LINE);
            out.flush();
        }
        return true;
    }

    // Returns true if no error were found
    // Returns false if there was no response
    private static boolean processServerResponse(String response, Socket socket) throws IOException {
        //if response == null the server has disconnected
        if (response == null) {
            socket.close();
            return false;
        }
        String[] responseSplit = response.split(" ", 2);
        ServerCommand command = null;
        try{
            command = ServerCommand.valueOf(responseSplit[0]);
        } catch (Exception e){
            System.out.println("[Server] Unknown response: " + responseSplit[0]);
            return false;
        }

        String sender;
        String message;

        switch (command){
            case OK, ERROR:
                System.out.println("[Server] " + response);
                break;
            case RECEIVE_PRIVATE:
                String[] splitSenderAndMessage = responseSplit[1].split(" ", 2);
                if(splitSenderAndMessage.length < 2){return false;}
                sender = splitSenderAndMessage[0];
                message = splitSenderAndMessage[1];
                System.out.println("[" + sender +  "] " + message);
                break;
            case RECEIVE_GROUP:
                String[] splitGroupSenderMessage = responseSplit[1].split(" ", 3);
                if(splitGroupSenderMessage.length < 3){return false;}
                String group = splitGroupSenderMessage[0];
                sender = splitGroupSenderMessage[1];
                message = splitGroupSenderMessage[2];
                System.out.println("[" + group +  ":" + sender + "] " + message);
                break;
            case HISTORY:
                String[] messages = responseSplit[1].split("\\|"); // "|" is the message delimiter
                for (String messageHistory : messages){
                    System.out.println(" - " + messageHistory);
                }
                break;
            case LIST_GROUPS:
                String[] groups = responseSplit[1].split(" ");
                System.out.println();
                for (String currentGroup : groups){
                    System.out.println(" - " + currentGroup);
                }
                break;
        }
        System.out.print("> ");
        return true;
    }

    private static void getErrorFromCode(int code) {
    }

    static class ServerListener implements Runnable {
        private Socket socket;
        public ServerListener(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String response;
                while ((response = in.readLine()) != null) {
                    if (!processServerResponse(response, socket)) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("[ServerListener] Error reading from socket: " + e.getMessage());
            }
        }
    }
}