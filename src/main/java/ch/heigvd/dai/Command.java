package ch.heigvd.dai;

enum ClientCommand{
    JOIN,
}

enum ServerCommand{
    OK,
    ERROR,
}

enum ErrorCode{
    INVALID,
    USER_ALREADY_EXISTS
}