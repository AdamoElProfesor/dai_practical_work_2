package ch.heigvd.dai;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;


class Client {

    private static final String HOST = "localhost";
    private static final int PORT = 1234;
    public static String END_OF_LINE = "\n";
/*
    public enum ClientCommand{
        JOIN,
    }*/

    public enum ServerCommand{
        OK,
    }

    public static void main(String[] args) {
        System.out.println("[Client] Connecting to " + HOST + ":" + PORT + "...");

        try (Socket socket = new Socket(HOST, PORT);
             Reader reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(reader);
             Writer writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
             BufferedWriter out = new BufferedWriter(writer); ) {

            System.out.println("[Client] Connected to " + HOST + ":" + PORT);

            //Display all commands
            // help();


            while (!socket.isClosed()) {
                // Display prompt
                System.out.print("> ");

                // Read user input
                Reader inputReader = new InputStreamReader(System.in, StandardCharsets.UTF_8);
                BufferedReader bir = new BufferedReader(inputReader);
                String userInput = bir.readLine(); // blocking

                try{
                    String[] userInputParts = userInput.split(" ", 2);
                    ClientCommand command = ClientCommand.valueOf(userInputParts[0].toUpperCase()); // Throws exception if no match

                    String request = null;

                    switch (command){
                        case JOIN:
                            String name = userInputParts[1];
                            request = ClientCommand.JOIN + " " + name;
                            break;
                    }

                    //Useless condition for the moment
                    if (request != null) {
                        out.write(request + END_OF_LINE);
                        out.flush();
                    }


                } catch (Exception e){
                    System.out.println("[Client] Invalid command: " + userInput);
                    continue;
                }

                String response = in.readLine();

                //if response == null the server has disconnected
                if (response == null) {
                    socket.close();
                    continue;
                }


                String[] responseSplit = response.split(" ", 2);
                ServerCommand command = null;
                try{
                    command = ServerCommand.valueOf(responseSplit[0]);
                } catch (Exception e){
                    System.out.println("[Server] Unknown response: " + responseSplit[0]);
                }

                switch (command){
                    case OK:
                        String message = responseSplit[1];
                        System.out.println(message);
                        break;
                    case null: // Useless??
                        System.out.println("Invalid/unknown command sent by server, ignore.");
                }

            }
            System.out.println("[Client] Closing connection and quitting...");
        } catch (IOException e) {
            System.out.println("[Client] IO exception: " + e);
        }
    }
}