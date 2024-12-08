package ch.heigvd.dai.network;
import ch.heigvd.dai.util.*;
import picocli.CommandLine;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;


@CommandLine.Command(name = "server", description = "Start the server part of the networkd.")
public class Server implements Callable<Integer> {

    private CopyOnWriteArrayList<User> users = new CopyOnWriteArrayList<>();
    public  String END_OF_LINE = "\n";
    public  char MESSAGE_MAX_SIZE = 100;
    public  int MINIMUM_PORT = 1025;
    public  int MAXIMUM_PORT = 65535;

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to use (default: ${DEFAULT-VALUE}).",
            defaultValue = "1234")
    protected int PORT;

    @Override
    public Integer call(){
        if (PORT < MINIMUM_PORT || PORT > MAXIMUM_PORT) {
            System.err.println("Error: Port must be between " + MINIMUM_PORT + " " + "and " + MAXIMUM_PORT);
            return 1;
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[Server] Listening on port " + PORT);

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept(); // blocking
                Thread clientThread = new Thread(new ClientHandler(socket));
                clientThread.start();
            }
            System.out.println("[Server] Closing connection");
        } catch (IOException e) {
            System.out.println("[Server] IO exception: " + e);
        }
        return 0;
    }

    class ClientHandler implements Runnable {

        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (socket; // This allows to use try-with-resources with the socket
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))){
                System.out.println(
                        "[Server] New client connected from "
                                + socket.getInetAddress().getHostAddress()
                                + ":"
                                + socket.getPort());

                while (!socket.isClosed()) {
                    String userInput = in.readLine(); // blocking
                    if(userInput == null) { // Client disconnected
                        User.removeUserFromAddress(users, socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                        System.out.println(
                                "[Server] Leaving client from "
                                        + socket.getInetAddress().getHostAddress()
                                        + ":"
                                        + socket.getPort());
                        break;
                    }
                    processClientInput(userInput, socket, out);
                }
            } catch (IOException e) {
                System.out.println("[Server] exception: " + e);
            }
        }
    }


    private void processClientInput(String input, Socket socket, BufferedWriter out) throws IOException {
        if (input == null) return;
        String[] userInputSplit = input.split(" ", 2);

        ClientCommand command = null;
        try {
            command = ClientCommand.valueOf(userInputSplit[0]);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid/unknown command sent by client, ignore.");
            return;
        }

        /* Main logic when processing the userInput */
        switch (command) {
            case JOIN -> processJoin(userInputSplit, socket, out);
            case SEND_PRIVATE -> processSendPrivate(userInputSplit, socket, out);
            case SEND_GROUP -> processSendGroup(userInputSplit, socket, out);
            case PARTICIPATE -> processParticipate(userInputSplit, socket, out);
            case HISTORY -> processHistory(userInputSplit, socket, out);
            case LIST_GROUPS -> processListGroups(out);
            case LIST_USERS -> processListUsers(out);
        }
    }

    private void processJoin(String[] input,Socket socket, BufferedWriter out) throws IOException {
        String name = input[1];
        int errorCode = 0;

        /* Duplicate usernames */
        if(User.findUserByName(users, name) != null) {
            errorCode = 1;
            sendErrorResponse(out, errorCode);
            return;
        }

        // If username already has an account, remove it
        if(User.findUserByAddress(users, socket.getInetAddress().getHostAddress() + ":" + socket.getPort ()) != null) {
            User.removeUserFromAddress(users, socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
        }

        //Add new username
        User user = new User(name, socket.getInetAddress().getHostAddress() + ":" + socket.getPort(), out);
        users.add(user);
        System.out.println("[Server] New client joined " + name);
        sendOkResponse(out);
    }

    private void processSendPrivate(String[] input,Socket socket, BufferedWriter out) throws IOException {
        String recipient = input[1].split(" ", 2)[0];
        String content = input[1].split(" ", 2)[1];

        int errorCode = 0;

        // Check if sender is found
        User sender;
        if((sender = User.findUserByAddress(users, socket.getInetAddress().getHostAddress() + ":" + socket.getPort())) == null){
            errorCode = 3;
            sendErrorResponse(out, errorCode);
            return;
        }

        // Message too long
        if (content.length() > MESSAGE_MAX_SIZE){
            errorCode = 2;
            sendErrorResponse(out, errorCode);
            return;
        }

        // Check if recipient is found
        User user;
        if((user = User.findUserByName(users, recipient)) == null) {
            errorCode = 1;
            sendErrorResponse(out, errorCode);
            return;
        }
        user.getOutput().write(ServerCommand.RECEIVE_PRIVATE + " " + sender.getName() + " " + content + END_OF_LINE);
        user.getOutput().flush();
        sendOkResponse(out);
    }

    private void processSendGroup(String[] input,Socket socket, BufferedWriter out) throws IOException {
        String group = input[1].split(" ", 2)[0];
        String content = input[1].split(" ", 2)[1];

        int errorCode = 0;

        // Check if sender has a username
        User sender;
        if((sender = User.findUserByAddress(users, socket.getInetAddress().getHostAddress() + ":" + socket.getPort())) == null){
            errorCode = 3;
            sendErrorResponse(out, errorCode);
            return;
        }

        // Check if message too long
        if (content.length() > MESSAGE_MAX_SIZE){
            errorCode = 2;
            sendErrorResponse(out, errorCode);
            return;
        }

        // Check if valid group name
        if(!User.isValidGroupName(group)) {
            errorCode = 1;
            sendErrorResponse(out, errorCode);
            return;
        }

        // Check if user in the specific group
        if (!sender.isInGroup(group)){
            errorCode = 4;
            sendErrorResponse(out, errorCode);
            return;
        }

        User[] usersToSendMessage = User.findAllUsersByGroup(users, group);

        // Send message to all participants of the group
        for(User user : usersToSendMessage){
            if (user == sender) continue;
            user.getOutput().write(ServerCommand.RECEIVE_GROUP + " " + group + " " + sender.getName() + " " + content + END_OF_LINE);
            user.getOutput().flush();
        }

        // Writes message in the txt file
        String filePath = group.toLowerCase() + ".txt";
        try(BufferedWriter groupWriter = new BufferedWriter(new FileWriter(filePath, true))){
            groupWriter.write(sender.getName() + " " + content + END_OF_LINE);
            groupWriter.flush();
        }catch (IOException e){
            System.out.println("[Server] IO exception: " + e);
        }
        System.out.println("OUT");
        sendOkResponse(out);
    }

    private void processParticipate(String[] input,Socket socket, BufferedWriter out) throws IOException {
        String groupName = input[1];

        int errorCode = 0;

        // Check if sender exists
        User user;
        if((user = User.findUserByAddress(users, socket.getInetAddress().getHostAddress() + ":" + socket.getPort())) == null){
            errorCode = 2;
            sendErrorResponse(out, errorCode);
            return;
        }

        // Check if group name is valid
        if (!User.isValidGroupName(groupName)) {
            errorCode = 1;
            sendErrorResponse(out, errorCode);
            return;
        }

        user.addGroupToUser(groupName);
        System.out.println("[Server] " + user.getName() + " joined " + groupName);
        sendOkResponse(out);
    }

    private void processHistory(String[] input,Socket socket, BufferedWriter out) throws IOException {
        String groupName = input[1];

        int errorCode = 0;

        // Check if sender exists
        User user;
        if((user = User.findUserByAddress(users, socket.getInetAddress().getHostAddress() + ":" + socket.getPort())) == null){
            errorCode = 2;
            sendErrorResponse(out, errorCode);
            return;
        }

        if (!User.isValidGroupName(groupName)) {
            errorCode = 3; // NOT DEFINED IN THE APP PROTOCOL
            sendErrorResponse(out, errorCode);
            return;
        }

        if (!user.isInGroup(groupName)) {
            errorCode = 1;
            sendErrorResponse(out, errorCode);
            return;
        }

        // Retrieves all the content in the text file. ("|" as a separator)
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(groupName + ".txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if(content.length() > 1){
                    content.append("|");
                }
                content.append(line);
            }
        } catch (IOException e) {
            System.err.println("An error occurred while retrieving text content: " + e.getMessage());
        }
        out.write(ServerCommand.HISTORY + " " + content + END_OF_LINE);
        out.flush();
    }

    private void processListGroups(BufferedWriter out) throws IOException {
        String[] groups = User.getExistingGroups();
        StringBuilder response = new StringBuilder();
        for (String group : groups) {
            response.append(group).append(" ");
        }
        out.write(ServerCommand.LIST_GROUPS + " " + response + END_OF_LINE);
        out.flush();
    }

    private void processListUsers(BufferedWriter out) throws IOException {
        StringBuilder response = new StringBuilder();
        for (User user : users) {
            response.append(user.getName()).append(" ");
        }
        out.write(ServerCommand.LIST_USERS + " " + response + END_OF_LINE);
        out.flush();
    }


    private void sendOkResponse (BufferedWriter out) throws IOException {
        out.write("OK" + END_OF_LINE);
        out.flush();
    }

    private void sendErrorResponse(BufferedWriter out, int code) throws IOException {
        out.write("ERROR " + code + END_OF_LINE);
        out.flush();
    }
}


class User{
    private String name;
    private String address; // Maybe not useful
    private BufferedWriter output;
    private ArrayList<String> groups;
    private static final String[] existingGroups = {"HEIG-VD", "SPORT", "VOITURE"};

    public User(String name, String address, BufferedWriter output) {
        this.name = name;
        this.address = address;
        this.output = output;
        this.groups = new ArrayList<>();
    }
    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public BufferedWriter getOutput() {
        return output;
    }

    static public String[] getExistingGroups() {return existingGroups; }

    static public boolean isValidGroupName(String group){
        for (String currentGroup : existingGroups){
            if(group.equals(currentGroup)){
                return true;
            }
        }
        return false;
    }

    public void addGroupToUser(String group){
        this.groups.add(group);
    }

    public boolean isInGroup(String group){
        return this.groups.contains(group);
    }

    static public User[] findAllUsersByGroup(CopyOnWriteArrayList<User> users, String group){
        CopyOnWriteArrayList<User> usersInGroup = new CopyOnWriteArrayList<>();
        for (User user : users){
            if(user.isInGroup(group)){
                usersInGroup.add(user);
            }
        }
        return usersInGroup.toArray(new User[0]);
    }

    static public void removeUserFromAddress(CopyOnWriteArrayList<User> users, String address) {
        users.removeIf(user -> user.getAddress().equals(address));
    }

    /* returns -1 when user is not found*/
    static public User findUserByName(CopyOnWriteArrayList<User> users, String name) {
        for (User user : users) {
            if (user.getName().equals(name)) {
                return user;
            }
        }
        return null;
    }
    static public User findUserByAddress(CopyOnWriteArrayList<User> users, String address) {
        for (User user : users) {
            if (user.getAddress().equals(address)) {
                return user;
            }
        }
        return null;
    }
}