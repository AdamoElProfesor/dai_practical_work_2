package ch.heigvd.dai;

enum ClientCommand{
    JOIN,
    SEND_PRIVATE
}

enum ServerCommand{
    OK,
    ERROR,
    RECEIVE_PRIVATE
}

enum ErrorCode{
    INVALID,
    USER_ALREADY_EXISTS,
    USER_NOT_FOUND,
}