package ch.heigvd.dai.util;
import java.util.List;

public class ErrorMapping {
    private ClientCommand command;
    private int errorCode;
    private String message;
    static final List<ErrorMapping> errorMappings = List.of(
            new ErrorMapping(ClientCommand.JOIN, 1, "The username is already in use."),
            new ErrorMapping(ClientCommand.SEND_PRIVATE, 1, "The recipient is not connected."),
            new ErrorMapping(ClientCommand.SEND_PRIVATE, 2, "The message exceeds 100 characters."),
            new ErrorMapping(ClientCommand.SEND_PRIVATE, 3, "The client has not connected to the server using `JOIN`."),
            new ErrorMapping(ClientCommand.SEND_GROUP, 1, "The specified group does not exist."),
            new ErrorMapping(ClientCommand.SEND_GROUP, 2, "The message exceeds 100 characters."),
            new ErrorMapping(ClientCommand.SEND_GROUP, 3, "The client has not connected to the server using `JOIN`."),
            new ErrorMapping(ClientCommand.SEND_GROUP, 4, "The client is not a participant in the specified group."),
            new ErrorMapping(ClientCommand.PARTICIPATE, 1, "The specified group does not exist."),
            new ErrorMapping(ClientCommand.PARTICIPATE, 2, "The client has not connected to the server using `JOIN`."),
            new ErrorMapping(ClientCommand.HISTORY, 1,"The client is not a member of the specified group."),
            new ErrorMapping(ClientCommand.HISTORY, 2, "The client has not connected to the server using `JOIN`.")
            );

    public ErrorMapping(ClientCommand command, int errorCode, String message) {
        this.command = command;
        this.errorCode = errorCode;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public static ErrorMapping findErrorMapping(ClientCommand command, int errorCode) {
        // Search the errorMappings list for a matching command and error code
        for (ErrorMapping mapping : ErrorMapping.errorMappings) {
            if (mapping.command == command && mapping.errorCode == errorCode) {
                return mapping;
            }
        }
        return null;  // Return null if no match is found
    }

}

