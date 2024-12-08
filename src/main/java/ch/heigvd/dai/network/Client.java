package ch.heigvd.dai.network;

import ch.heigvd.dai.util.*;
import picocli.CommandLine;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "client", description = "Start the client part of the network .")
public class Client implements Callable<Integer> {
    public String END_OF_LINE = "\n";
    private ClientCommand lastCommand = null;
    public int MINIMUM_PORT = 1025;
    public int MAXIMUM_PORT = 65535;

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to use (default: ${DEFAULT-VALUE}).",
            defaultValue = "1234")
    protected int PORT;

    @CommandLine.Option(
            names = {"-h", "--host"},
            description = "Host to use(default: ${DEFAULT-VALUE}).",
            defaultValue = "localhost")
    protected String HOST;

    @Override
    public Integer call(){
        if (PORT < MINIMUM_PORT || PORT > MAXIMUM_PORT) {
            System.err.println("Error: Port must be between " + MINIMUM_PORT + " " + "and " + MAXIMUM_PORT);
            return 1;
        }

        System.out.println("[Client] Connecting to " + HOST + ":" + PORT + "...");

        try (Socket socket = new Socket(HOST, PORT);

             Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(reader);

             Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
             BufferedWriter out = new BufferedWriter(writer)) {

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
        return 0;
    }

    // Returns true if no errors were found
    // Returns false if the client input was invalid / unknown
    private void processClientInput(String input, BufferedWriter out) throws IOException {
        if (input == null) return;
        String[] userInputParts = input.split(" ", 2);
        ClientCommand command = null;
        try{
            command = ClientCommand.valueOf(userInputParts[0].toUpperCase()); // Throws exception if no match
        } catch (Exception e){
            System.out.println("[Client] Invalid command: " + input);
            return;
        }
        lastCommand = command;

        String request = switch (command) {
            case JOIN -> requestJoin(userInputParts);
            case SEND_PRIVATE -> requestSendPrivate(userInputParts);
            case SEND_GROUP -> requestSendGroup(userInputParts);
            case PARTICIPATE -> requestParticipate(userInputParts);
            case HISTORY -> requestHistory(userInputParts);
            case LIST_GROUPS -> requestListGroups(userInputParts);
            case LIST_USERS -> requestListUsers(userInputParts);
        };

        //Useless condition for the moment
        if (request != null) {
            out.write(request + END_OF_LINE);
            out.flush();
        }
    }

    private String requestJoin(String[] userInput){
        if (userInput.length != 2 || userInput[1].isEmpty()){
            System.out.println("[Client] Error on parameters");
            return null;
        }
        String name = userInput[1];
        return ClientCommand.JOIN + " " + name;
    }

    private String requestSendPrivate(String[] userInput){
        if(userInput[1].split(" ").length < 2){
            System.out.println("[Client] Error when sending message");
            return null;
        }

        String recipient = userInput[1].split(" ", 2)[0];
        String content = userInput[1].split(" ", 2)[1];

        return ClientCommand.SEND_PRIVATE + " " + recipient + " " + content;
    }
    private String requestSendGroup(String[] userInput){
        if(userInput[1].split(" ").length < 2){
            System.out.println("[Client] Error when sending message");
            return null;
        }
        String group = userInput[1].split(" ", 2)[0];
        String message = userInput[1].split(" ", 2)[1];
        return ClientCommand.SEND_GROUP + " " + group.toUpperCase() + " " + message;
    }
    private String requestParticipate(String[] userInput){
        if (userInput.length != 2 || userInput[1].isEmpty()){
            System.out.println("[Client] Error on parameters");
            return null;
        }
        String groupName = userInput[1];
        return ClientCommand.PARTICIPATE + " " + groupName.toUpperCase();
    }
    private String requestHistory(String[] userInput){
        if (userInput.length != 2 || userInput[1].isEmpty()){
            System.out.println("[Client] Error on parameters");
            return null;
        }
        String groupNameHistory = userInput[1];
        return ClientCommand.HISTORY + " " + groupNameHistory.toUpperCase();
    }
    private String requestListGroups(String[] userInput){
        if (userInput.length != 1){
            System.out.println("[Client] Error on parameters");
            return null;
        }
         return ClientCommand.LIST_GROUPS + "";
    }
    private String requestListUsers(String[] userInput){
        if (userInput.length != 1){
            System.out.println("[Client] Error on parameters");
            return null;
        }
        return ClientCommand.LIST_USERS + "";
    }

    // Returns true if no error were found
    // Returns false if there was no response
    private boolean processServerResponse(String response, Socket socket) throws IOException {
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

        switch (command){
            case OK ->responseOk();
            case ERROR -> responseError(responseSplit);
            case RECEIVE_PRIVATE -> responseReceivePrivate(responseSplit);
            case RECEIVE_GROUP -> responseReceiveGroup(responseSplit);
            case HISTORY -> responseHistory(responseSplit);
            case LIST_GROUPS, LIST_USERS -> responseList(responseSplit); // when the response is a list of something
        }
        System.out.print("> ");
        return true;
    }

    private void responseOk(){
        switch (lastCommand){
            case JOIN: System.out.println("[Client] You successfully joined"); break;
            case SEND_PRIVATE, SEND_GROUP: System.out.println("[Client] Message successfully sent"); break;
            case PARTICIPATE: System.out.println("[Client] You successfully joined the group"); break;
        }
    }

    private void responseError(String[] response){
        ErrorMapping err;
        if((err = ErrorMapping.findErrorMapping(lastCommand, Integer.parseInt(response[1])))== null){
            System.out.println("[Server] Unknown error code ");
            return;
        }
        System.out.println("[Server] " + err.getMessage());
    }

    private void responseReceivePrivate(String[] response){
        String sender;
        String message;

        if(response[1].split(" ", 2).length < 2){
            System.out.println("[Client] Couldn't properly receive private message");
            return;
        }
        sender = response[1].split(" ", 2)[0];
        message = response[1].split(" ", 2)[1];

        System.out.println("[" + sender +  "] " + message);
    }

    private void responseReceiveGroup(String[] response){
        String sender;
        String message;

        if(response[1].split(" ", 3).length < 3){
            System.out.println("[Client] Couldn't properly receive group message");
            return;
        }
        String group = response[1].split(" ", 3)[0];
        sender = response[1].split(" ", 3)[1];
        message = response[1].split(" ", 3)[2];

        System.out.println("[" + group +  ":" + sender + "] " + message);
    }

    private void responseHistory(String[] response){
        String[] messages = response[1].split("\\|"); // "|" is the message delimiter
        for (String messageHistory : messages){
            System.out.println(" - " + messageHistory);
        }
    }

    private void responseList(String[] response){
        String[] list = response[1].split(" ");
        System.out.println();
        for (String item : list){
            System.out.println(" - " + item);
        }
    }



    class ServerListener implements Runnable {
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