package ch.heigvd.dai;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Server {
    private static final int PORT = 1234;
    private static ArrayList<User> users = new ArrayList<>();

    public static String END_OF_LINE = "\n";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[Server] Listening on port " + PORT);

            while (!serverSocket.isClosed()) {
                try (Socket socket = serverSocket.accept(); // blocking
                     Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
                     BufferedReader in = new BufferedReader(reader);
                     Writer writer =
                             new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
                     BufferedWriter out = new BufferedWriter(writer)) {
                    System.out.println(
                            "[Server] New client connected from "
                                    + socket.getInetAddress().getHostAddress()
                                    + ":"
                                    + socket.getPort());

                    while (!socket.isClosed()) {
                        String userInput = in.readLine(); // blocking
                        if(userInput == null) { // Client disconnected
                            User.removeUsersFromAddress(users, socket.getInetAddress().getHostAddress());
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
                    System.out.println("[Server] IO exception: " + e);
                }
            }
            System.out.println("[Server] Closing connection");
        } catch (IOException e) {
            System.out.println("[Server] IO exception: " + e);
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
                User user = new User(name, socket.getInetAddress().getHostAddress(), out);
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
                sendOkResponse(out);
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

    static public boolean doesNameExistsInUsers(ArrayList<User> users, String name) {
        for (User user : users) {
            if (user.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    static public void removeUsersFromAddress(ArrayList<User> users, String address) {
        users.removeIf(user -> user.getAddress().equals(address));
    }
}