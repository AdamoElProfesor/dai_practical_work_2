package ch.heigvd.dai;

enum ClientCommand{
    JOIN,
}

enum ServerCommand{
    OK,
    ERROR,
}

enum ErrorCode{
    USER_ALREADY_EXISTS;
}