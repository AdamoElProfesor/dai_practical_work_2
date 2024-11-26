package ch.heigvd.dai;

enum ClientCommand{
    JOIN,
    SEND_PRIVATE,
    SEND_GROUP,
    PARTICIPATE,
    HISTORY
}

enum ServerCommand{
    OK,
    ERROR,
    RECEIVE_PRIVATE,
    RECEIVE_GROUP,
    HISTORY
}

enum ErrorCode{
    INVALID,
    USER_ALREADY_EXISTS,
    USER_NOT_FOUND,
    GROUP_NOT_FOUND
}