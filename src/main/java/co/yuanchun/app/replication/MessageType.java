package co.yuanchun.app.replication;

public class MessageType {
    public static final String INSERT_CONFIRMATION = "insert-success"; 
    public static final String INSERT_REQUEST = "db-insert";
    public static final String INSERT_FAILURE = "insert-failure";
    public static final String CMD_CLOSE = "cmd-close";

}