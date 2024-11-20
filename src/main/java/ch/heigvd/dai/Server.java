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
                try (Socket socket = serverSocket.accept();
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
        }

        switch (command) {
            case JOIN -> {
                String name = userInputSplit[1];
                User user = new User(name, socket.getInetAddress().getHostAddress());
                users.add(user);
                System.out.println("[Server] New client joined " + name);
                out.write("OK" + END_OF_LINE);
                out.flush();
            }
            case null -> {
                System.out.println("[Server] Invalid command sent by client, ignore.");
            }
        }
    }
}

class User{
    private String name;
    private String address;

    public User(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}