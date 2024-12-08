# Network Application

## Purpose
This network application allows users to communicate through private and group chats, with the ability to load chat histories stored in text files. It is designed to accommodate small group interactions, with features that enhance communication efficiency and data retrieval.

Key features include:
- Private and group messaging.
- Group chat history retrieval.
- User-friendly commands for interacting with the server.

# Usage Instructions

## Using the Network Application

Once the server is running and the client is connected, users can interact with the application using the following commands:

- **Join the server**  
  Allows a user to join the server by providing a unique username.  
  **Command:**
  ```plaintext
  JOIN <username>
  ```  
  **Example:**
  ```plaintext
  Request: JOIN Alice
  Response: You successfully joined
  ```

- **Send a private message**  
  Sends a message to a specific recipient.  
  **Command:**
  ```plaintext
  SEND_PRIVATE <recipient> <message>
  ```  
  **Example:**
  ```plaintext
  Request: SEND_PRIVATE Bob Hello!
  Response: OK
  ```

- **Send a group message**  
  Sends a message to all members of a specific group.  
  **Command:**
  ```plaintext
  SEND_GROUP <group> <message>
  ```  
  **Example:**
  ```plaintext
  Request: SEND_GROUP Sport Hi everyone!
  Response: OK
  ```

- **Retrieve chat history**  
  Loads and displays the chat history for a specific group.  
  **Command:**
  ```plaintext
  HISTORY <group>
  ```  
  **Example:**
  ```plaintext
  Request: HISTORY dev-team
  Response: HISTORY Alice: Hi team! Bob: Hello Alice!
  ```

## Example Outputs

- **Successful operations** return:
  ```plaintext
  OK
  ```

- **Errors** return descriptive messages:
  ```plaintext
  ERROR <code>: <error description>
  ```  

These examples allow users and developers to understand how to interact with the network application without needing to run it locally.
## Find all commands
If you want to see all the possible commands, you can check out the application protocol in [doc/application_protocol.md](doc/application_protocol.md)
# How to Run the App Using Docker

## Prerequisites
Before running the app, ensure that the following are installed on your machine:

- **Docker**: Download and install Docker from [here](https://www.docker.com/get-started).
- **Git**: Download and install Git from [here](https://git-scm.com/).

## Cloning the Repository
Clone the repository to your local machine using the following command:

```bash
git clone https://github.com/AdamoElProfesor/dai_practical_work_2.git
```


Navigate to the project directory:

```bash
cd dai_practical_work_2
```

## Build the Project
If you want to build the project manually before running the Docker container, you can do so by using the Maven Wrapper.

Make sure the Maven Wrapper is available in the project directory. You can check by running:

```bash
./mvnw -v
```

To build the project, run the following command:

```bash
./mvnw clean install
```

This will build the project and create the `.jar` file that will be used inside the Docker container.

## Running the App Using Docker

1. **Build the Docker Image**  
   If you haven't already built the Docker image, run the following command in the root of the project directory (where the Dockerfile is located):

   ```bash
   docker build -t msg_app .
   ```

   This will create a Docker image named `msg_app`.


2. **Create a Docker Network (Manual Step)**  
   Before running the containers, you'll need to create a Docker network. This allows your server and client containers to communicate with each other.

   To create a network, run the following command:

   ```bash
   docker network create network1
   ```

3. **Run the Server**  
   Start the server by running the following command:

   ```bash
   docker run --name my-server --network network1 -p 1234:1234 msg_app
   ```

   This command does the following:
    - Runs the Docker container as `my-server`.
    - Exposes port 1234 of the container to port 1234 on your machine.
    - Connects the container to the Docker network (`network1`).

   **Server Parameters**
    - `-p <port>`:  Specifies the port on which the server is listening. Default is `1234`.


4. **Run the Client**  
   Once the server is running, open another terminal window and run the client:

   ```bash
   docker run -it --name my-client1 --network network1 msg_app client -h my-server -p 1234
   ```

   This command will:
    - Start the client container and connect it to the same network (`network1`) as the server.
    - Use the server's hostname (`my-server`) and port (`1234`).

   **Client Parameters**
    - `-h <host>`: Specifies the hostname or IP address of the server container. The default is `localhost`, but here it's set to `my-server`.
    - `-p <port>`: Specifies the port the client must connect. Default is `1234`.

If you want to run multiple clients, you need to have a different name for every client. (The --name param should be different)



5. **Stop the Containers**  
   To stop the server and client, use the following commands:

   ```bash
   docker stop my-server
   docker stop my-client
   ```

   If you want to remove the containers after stopping them:

   ```bash
   docker rm my-server
   docker rm my-client
   ```
   If you had another client with a different name, don't forget to remove it with his given name


6. **Cleanup**  
   To remove the Docker network after you're done, run:

   ```bash
   docker network rm network1
   ```
