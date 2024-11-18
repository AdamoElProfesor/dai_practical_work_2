package ch.heigvd.dai;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

class TcpTextualClient {

    private static final String HOST = "localhost";
    private static final int PORT = 1234;

    private static final String TEXTUAL_DATA = "Test message : Hi from client";

    public static String END_OF_LINE = "\n";

    public static void main(String[] args) {
        System.out.println("[Client] Connecting to " + HOST + ":" + PORT + "...");

        try (Socket socket = new Socket(HOST, PORT);
             Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(reader);
             Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
             BufferedWriter out = new BufferedWriter(writer); ) {
            System.out.println("[Client] Connected to " + HOST + ":" + PORT);
            System.out.println(
                    "[Client] Sending textual data to server " + HOST + ":" + PORT + ": " + TEXTUAL_DATA);

            out.write(TEXTUAL_DATA + END_OF_LINE);
            out.flush();

            System.out.println("[Client] Received textual data from server: " + in.readLine());
            System.out.println("[Client] Closing connection...");
        } catch (IOException e) {
            System.out.println("[Client] IO exception: " + e);
        }
    }
}