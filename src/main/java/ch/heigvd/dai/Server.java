package ch.heigvd.dai;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private static final int PORT = 1234;
    private static CopyOnWriteArrayList<User> users = new CopyOnWriteArrayList<>();

    public static String END_OF_LINE = "\n";

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
                        User.removeUserFromAddress(users, socket.getInetAddress().getHostAddress());
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

        switch (command) {
            case JOIN -> {
                String name = userInputSplit[1];
                if(User.doesNameExistsInUsers(users, name)) {
                    System.out.println("[Server] Name already exists");
                    sendErrorResponse(out, ErrorCode.USER_ALREADY_EXISTS);
                    return;
                }
                User user = new User(name, socket.getInetAddress().getHostAddress() + ":" + socket.getPort(), out);
                users.add(user);
                System.out.println("[Server] New client joined " + name);
                sendOkResponse(out);
            }
            case SEND_PRIVATE -> {
                String recipient = userInputSplit[1].split(" ", 2)[0];
                String content = userInputSplit[1].split(" ", 2)[1];
                if (!User.doesNameExistsInUsers(users, recipient)){
                    System.out.println("[Server] User is not connected or doesn't exist");
                    sendErrorResponse(out, ErrorCode.USER_NOT_FOUND);
                    return;
                }
                int index = User.findUserIndexByName(users, recipient);
                User sender = User.findUserByAddress(users, socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                if(sender == null){
                    System.out.println("[Server] User is not connected or doesn't exist");
                    sendErrorResponse(out, ErrorCode.USER_NOT_FOUND);
                    return;
                }
                if(index == -1) { // User is not found
                    sendErrorResponse(out, ErrorCode.USER_NOT_FOUND);
                    return;
                }
                User user = users.get(index);
                user.getOutput().write("RECEIVE_PRIVATE " + sender.getName() + " " + content + END_OF_LINE);
                user.getOutput().flush();
                sendOkResponse(out);
                break;
            }
        }
    }
    private static void sendOkResponse (BufferedWriter out) throws IOException {
        out.write("OK" + END_OF_LINE);
        out.flush();
    }

    private static void sendErrorResponse(BufferedWriter out, ErrorCode code) throws IOException {
        out.write("ERROR " + code.ordinal() + END_OF_LINE);
        out.flush();
    }
}

class User{
    private String name;
    private String address; // Maybe not useful
    private BufferedWriter output;

    public User(String name, String address, BufferedWriter output) {
        this.name = name;
        this.address = address;
        this.output = output;
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