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
  Response: OK
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



# Cloning and Building the Network Application

## Cloning the Project

To get started, you need to clone the repository to your local machine. Run the following command:
```bash
git clone https://github.com/AdamoElProfesor/dai_practical_work_2.git
```
This will create a local copy of the project on your machine.

## Building the project with Maven

After cloning the project, navigate to the project's root directory:
```bash
cd TODO A DEFINIR
```

Make sure you have [Maven](https://maven.apache.org/) installed on your system. If Maven is not installed, you can download and install it from the official website

Once Maven is installed, build the project by running the following command:
```bash
mvn clean install
```

This will compile the application and resolve any dependencies. After the build process is completed, you will be able to run the server and client applications.