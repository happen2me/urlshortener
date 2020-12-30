package co.yuanchun.app.communication;

public class MessageType {
    public static final String INSERT_CONFIRMATION = "insert-success"; 
    public static final String INSERT_REQUEST = "db-insert";
    public static final String INSERT_FAILURE = "insert-failure";
    public static final String CMD_CLOSE = "cmd-close";
    public static final String FAILURE = "failure";
    public static final String READ_FORWARD_REQUEST = "read-forward-request";
    public static final String READ_FORWARD_CONFIRMATION = "read-forward-response";
    public static final String READ_FORWARD_NOT_FOUND = "read-forward-not-found";
}
