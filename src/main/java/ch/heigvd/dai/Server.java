package ch.heigvd.dai;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;



public class Server {

    private static final int PORT = 1234;
    private static CopyOnWriteArrayList<User> users = new CopyOnWriteArrayList<>();
    public static String END_OF_LINE = "\n";
    public static char MESSAGE_MAX_SIZE = 100;

    public static void main(String[] args) {
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
    }

    static class ClientHandler implements Runnable {

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


    private static void processClientInput(String input, Socket socket, BufferedWriter out) throws IOException {
        String[] userInputSplit = input.split(" ", 2);

        ClientCommand command = null;
        try {
            command = ClientCommand.valueOf(userInputSplit[0]);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid/unknown command sent by client, ignore.");
            return;
        }

        int errorCode = 0;
        switch (command) {
            case JOIN -> {
                String name = userInputSplit[1];
                if(User.doesNameExistsInUsers(users, name)) {
                    errorCode = 1;
                    System.out.println("[Server] Name already exists");
                    sendErrorResponse(out, errorCode);
                    return;
                }
                if(User.findUserByAddress(users, socket.getInetAddress().getHostAddress() + ":" + socket.getPort ()) != null) {
                    User.removeUserFromAddress(users, socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                }
                User user = new User(name, socket.getInetAddress().getHostAddress() + ":" + socket.getPort(), out);
                users.add(user);
                System.out.println("[Server] New client joined " + name);
                sendOkResponse(out);
            }
            case SEND_PRIVATE -> {
                String recipient = userInputSplit[1].split(" ", 2)[0];
                String content = userInputSplit[1].split(" ", 2)[1];

                // Check if sender has a username
                User sender;
                if((sender = User.findUserByAddress(users, socket.getInetAddress().getHostAddress() + ":" + socket.getPort())) == null){
                    System.out.println("[Server] Sender does not exist");
                    errorCode = 3;
                    sendErrorResponse(out, errorCode);
                    return;
                }

                if (content.length() > MESSAGE_MAX_SIZE){
                    System.out.println("[Server] The message sent is too long");
                    errorCode = 2;
                    sendErrorResponse(out, errorCode);
                    return;
                }

                /* This check is redundant with the bottom one ?*/
                if (!User.doesNameExistsInUsers(users, recipient)){
                    System.out.println("[Server] User is not connected or doesn't exist");
                    errorCode = 1;
                    sendErrorResponse(out, errorCode);
                    return;
                }

                /* Could be simplified by returning user ?*/
                int index = User.findUserIndexByName(users, recipient);

                if(index == -1) { // User is not found
                    errorCode = 1;
                    sendErrorResponse(out, errorCode);
                    return;
                }
                User user = users.get(index);
                user.getOutput().write(ServerCommand.RECEIVE_PRIVATE + " " + sender.getName() + " " + content + END_OF_LINE);
                user.getOutput().flush();
                sendOkResponse(out);
                break;
            }
            case SEND_GROUP -> {
                String group = userInputSplit[1].split(" ", 2)[0];
                String content = userInputSplit[1].split(" ", 2)[1];

                // Check if sender has a username
                User sender;
                if((sender = User.findUserByAddress(users, socket.getInetAddress().getHostAddress() + ":" + socket.getPort())) == null){
                    System.out.println("[Server] Sender does not exist");
                    errorCode = 3;
                    sendErrorResponse(out, errorCode);
                    return;
                }

                if (content.length() > MESSAGE_MAX_SIZE){
                    System.out.println("[Server] The message sent is too long");
                    errorCode = 2;
                    sendErrorResponse(out, errorCode);
                    return;
                }

                if(!User.isValidGroupName(group)) {
                    System.out.println("[Server] Group doesn't exist");
                    errorCode = 1;
                    sendErrorResponse(out, errorCode);
                    return;
                }

                if (!sender.isInGroup(group)){
                    System.out.println("[Server] User is not in specified group");
                    errorCode = 4;
                    sendErrorResponse(out, errorCode);
                    return;
                }

                User[] usersToSendMessage = User.findAllUsersByGroup(users, group);

                for(User user : usersToSendMessage){
                    if (user == sender) continue;
                    user.getOutput().write(ServerCommand.RECEIVE_GROUP + " " + group + " " + sender.getName() + " " + content + END_OF_LINE);
                    user.getOutput().flush();
                }
                String filePath = group.toLowerCase() + ".txt";

                try(BufferedWriter groupWriter = new BufferedWriter(new FileWriter(filePath, true))){
                    groupWriter.write(sender.getName() + " " + content + END_OF_LINE);
                    groupWriter.flush();
                }catch (IOException e){
                    System.out.println("[Server] IO exception: " + e);
                }
                System.out.println("OUT");
                sendOkResponse(out);
                break;
            }
            case PARTICIPATE -> {
                String groupName = userInputSplit[1];

                // Check if sender has a username
                User user;
                if((user = User.findUserByAddress(users, socket.getInetAddress().getHostAddress() + ":" + socket.getPort())) == null){
                    System.out.println("[Server] Sender does not exist");
                    errorCode = 2;
                    sendErrorResponse(out, errorCode);
                    return;
                }

                if (!User.isValidGroupName(groupName)) {
                    System.out.println("[Server] Group doesn't exist");
                    errorCode = 1;
                    sendErrorResponse(out, errorCode);
                    return;
                }

                user.addGroupToUser(groupName);
                System.out.println("[Server] " + user.getName() + " joined " + groupName);
                sendOkResponse(out);
                break;
            }
            case HISTORY -> {
                String groupName = userInputSplit[1];

                // Check if sender has a username
                User user;
                if((user = User.findUserByAddress(users, socket.getInetAddress().getHostAddress() + ":" + socket.getPort())) == null){
                    System.out.println("[Server] Sender does not exist");
                    errorCode = 2;
                    sendErrorResponse(out, errorCode);
                    return;
                }

                if (!User.isValidGroupName(groupName)) {
                    System.out.println("[Server] Group doesn't exist");
                    errorCode = 3; // NOT DEFINED IN THE APP PROTOCOL
                    sendErrorResponse(out, errorCode);
                    return;
                }

                if (!user.isInGroup(groupName)) {
                    System.out.println("[Server] User is not in specified group");
                    errorCode = 1;
                    sendErrorResponse(out, errorCode);
                    return;
                }

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
                    System.err.println("An error occurred: " + e.getMessage());
                }
                out.write(ServerCommand.HISTORY + " " + content + END_OF_LINE);
                out.flush();
            }
            case LIST_GROUPS -> {
                String[] groups = User.getExistingGroups();
                StringBuilder response = new StringBuilder();
                for (String group : groups) {
                    response.append(group).append(" ");
                }
                out.write(ServerCommand.LIST_GROUPS + " " + response + END_OF_LINE);
                out.flush();
            }
            case LIST_USERS -> {
                StringBuilder response = new StringBuilder();
                for (User user : users) {
                    response.append(user.getName()).append(" ");
                }
                out.write(ServerCommand.LIST_USERS + " " + response + END_OF_LINE);
                out.flush();
            }
        }
    }
    private static void sendOkResponse (BufferedWriter out) throws IOException {
        out.write("OK" + END_OF_LINE);
        out.flush();
    }

    private static void sendErrorResponse(BufferedWriter out, int code) throws IOException {
        out.write("ERROR " + code + END_OF_LINE);
        out.flush();
    }
}


class User{
    private String name;
    private String address; // Maybe not useful
    private BufferedWriter output;
    private ArrayList<String> groups;
    public static final String[] existingGroups = {"HEIG-VD", "SPORT", "VOITURE"};

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

    static public boolean doesNameExistsInUsers(CopyOnWriteArrayList<User> users, String name) {
        for (User user : users) {
            if (user.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    static public void removeUserFromAddress(CopyOnWriteArrayList<User> users, String address) {
        users.removeIf(user -> user.getAddress().equals(address));
    }

    /* returns -1 when user is not found*/
    static public int findUserIndexByName(CopyOnWriteArrayList<User> users, String name) {
        for (User user : users) {
            if (user.getName().equals(name)) {
                return users.indexOf(user);
            }
        }
        return -1;
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