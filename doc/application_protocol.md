# Application Protocol

## Overview
Our application protocol is a communication protocol that allows the sending of text messages (generally short) between different users.

## Transport Protocol

### 1. Overview
The protocol uses **TCP** to ensure reliable data transmission and operates on **port 1234**.  
Every message is:
- Encoded in **UTF-8**.
- Delimited by a new line character (`\n`).
- Treated as a text message.

### 2. Connection Establishment
#### 2.1. Client Connection
- The client must initiate the connection to the server.
- After connecting, the client provides a **username**.

#### 2.2. Username Verification
- The server verifies that the provided username is **not already in use**:
    - **If valid**: The client is allowed to join the server.
    - **If invalid**: The server denies access and sends an error message.

## Messaging
#### 3.1. Direct Messaging
- Once connected, the client can send a **text message** to the server specifying the **recipient**.
- **Server Verification**:
    - The recipient must be connected.
    - The message must not exceed **100 characters**.
    - **If valid**: The server delivers the message to the recipient.
    - **If invalid**: The server sends an error message to the sender.

#### 3.2. Group Messaging
##### 3.2.1. Listing Users and Groups
- A client can:
    - List all connected users.
    - List all available groups.

##### 3.2.2. Joining a Group
- A client can participate in one or multiple groups.
- **Server Verification**:
    - The group must exist.
    - **If valid**: The client is allowed to join the group.
    - **If invalid**: The server sends an error message.

##### 3.2.3. Sending Messages to a Group
- A client can send a **text message** to a group.
- **Server Verification**:
    - The group must exist.
    - The message must not exceed **100 characters**.
    - **If valid**: The server broadcasts the message to all group members.
    - **If invalid**: The server sends an error message to the sender.

#### 3.3. Viewing Group Chat History
- A client can request the **chat history** of a group.
- **Server Verification**:
    - The client must be a member of the group.
    - **If valid**: The server sends the group chat history.
    - **If invalid**: The server sends an error message.

### 4. Error Handling
- For unknown or invalid messages, the server must send an **error message** to the client.

### 5. Disconnection
- When a client disconnects:
    - The server must **close the connection**.
    - The client is removed from the list of connected users.

## Messages

### Join the Server
The client sends a request to the server to join by providing a username.

**Client Input**  
`JOIN <name>`
- `<name>`: The desired username of the client.

**Server Output**
- `OK`: The client has been successfully granted access to the server.
- `ERROR <code>`: An error occurred during the join process.
    - **Error Codes**:
        - `1`: The username is already in use.

---

### Send a Private Message
The client sends a direct message to another user through the server.

**Client Input**  
`SEND_PRIVATE <recipient> <message>`
- `<recipient>`: The username of the recipient.
- `<message>`: The text message to be sent (must not exceed 100 characters).

**Server Output**
- `OK`: The message has been successfully sent.
- `ERROR <code>`: An error occurred while sending the message.
    - **Error Codes**:
        - `1`: The recipient is not connected.
        - `2`: The message exceeds 100 characters.
        - `3`: The client has not connected to the server using `JOIN`.



**Server to Recipient**  
`RECEIVE_PRIVATE <sender> <message>`
- `<sender>`: The username of the sender.
- `<message>`: The text message sent.

---

### Send a Group Message
The client sends a message to a group through the server.

**Client Input**  
`SEND_GROUP <group> <message>`
- `<group>`: The name of the group to send the message to.
- `<message>`: The text message to be sent (must not exceed 100 characters).

**Server Output**
- `OK`: The message has been successfully sent to the group.
- `ERROR <code>`: An error occurred while sending the message.
    - **Error Codes**:
        - `1`: The specified group does not exist.
        - `2`: The message exceeds 100 characters.
        - `3`: The client has not connected to the server using `JOIN`.
        - `4`: The client is not a participant in the specified group.

**Server to Group Members**  
`RECEIVE_GROUP <group> <sender> <message>`
- `<group>`: The name of the group.
- `<sender>`: The username of the sender.
- `<message>`: The text message sent.

---

### Participate in a Group
The client sends a request to join a specific group.

**Client Input**  
`PARTICIPATE <group>`
- `<group>`: The name of the group to join.

**Server Output**
- `OK`: The client has been successfully added to the group.
- `ERROR <code>`: An error occurred while attempting to join the group.
    - **Error Codes**:
        - `1`: The specified group does not exist.
        - `2`: The client has not connected to the server using `JOIN`.
---

### View Group Chat History
The client requests the chat history of a specific group.

**Client Input**  
`HISTORY <group>`
- `<group>`: The name of the group whose history is being requested.

**Server Output**
- `HISTORY <message1> <message2> ...`: A list of past messages from the group, in chronological order.   
  Each message is formatted as `<sender>: <text>`. (messages are delimited with the "|" character)  
  For example:
    - `¨axel salut mec|adam ça va et toi?|axel ça va aussi bien merci`
  

- `ERROR <code>`: An error occurred while retrieving the chat history.
    - **Error Codes**:
        - `1`: The client is not a member of the specified group.
        - `2`: The client has not connected to the server using `JOIN`.
---

### List Available Groups
The client requests a list of all available groups.

**Client Input**  
`LIST_GROUPS`

**Server Output**
- `LIST_GROUPS <group1> <group2> ...`: A space-separated list of all available groups.

---

### List Connected Users
The client requests a list of all connected users.

**Client Input**  
`LIST_USERS`

**Server Output**
- `LIST_USERS <user1 name> <user2 name> ...`: A space-separated list of all connected users.

--- 


### Unknown commands
- For unrecognized or invalid commands, the server returns:
  `ERROR 99`: Unknown command.

## Examples

[![Diagram Image Link](https://tinyurl.com/29h64tsf)](https://tinyurl.com/29h64tsf)<!--![Diagram Image Link](./join_server.puml)-->

[![Diagram Image Link](https://tinyurl.com/2afyo59c)](https://tinyurl.com/2afyo59c)<!--![Diagram Image Link](./send_private_message.puml)-->

[![Diagram Image Link](https://tinyurl.com/2captm46)](https://tinyurl.com/2captm46)<!--![Diagram Image Link](./participate_group.puml)-->

[![Diagram Image Link](https://tinyurl.com/26njd4jo)](https://tinyurl.com/26njd4jo)<!--![Diagram Image Link](./send_group_message.puml)-->

[![Diagram Image Link](https://tinyurl.com/25pnqt5r)](https://tinyurl.com/25pnqt5r)<!--![Diagram Image Link](./group_history.puml)-->

[![Diagram Image Link](https://tinyurl.com/23jn3vhc)](https://tinyurl.com/23jn3vhc)<!--![Diagram Image Link](./quit_server.puml)-->

### Errors

[![Diagram Image Link](https://tinyurl.com/235ywjgw)](https://tinyurl.com/235ywjgw)<!--![Diagram Image Link](errors-plantUML/error_user_duplicate.puml)-->

[![Diagram Image Link](https://tinyurl.com/2asn9v7w)](https://tinyurl.com/2asn9v7w)<!--![Diagram Image Link](errors-plantUML/error_message_exceeds_100_characters.puml)-->

[![Diagram Image Link](https://tinyurl.com/2547atvm)](https://tinyurl.com/2547atvm)<!--![Diagram Image Link](errors-plantUML/error_recipient_not_connected.puml)-->

[![Diagram Image Link](https://tinyurl.com/269qzfoa)](https://tinyurl.com/269qzfoa)<!--![Diagram Image Link](errors-plantUML/error_no_group_participant.puml)-->

[![Diagram Image Link](https://tinyurl.com/24e2cvtg)](https://tinyurl.com/24e2cvtg)<!--![Diagram Image Link](errors-plantUML/error_group_non-existent.puml)-->

[![Diagram Image Link](https://tinyurl.com/2yjkbgfq)](https://tinyurl.com/2yjkbgfq)<!--![Diagram Image Link](errors-plantUML/error_unknown.puml)-->
