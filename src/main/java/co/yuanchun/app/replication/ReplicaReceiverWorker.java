package co.yuanchun.app.replication;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import co.yuanchun.app.DatabaseAdaper;
import co.yuanchun.app.communication.MessageReceiverWorker;
import co.yuanchun.app.communication.MessageType;

public class ReplicaReceiverWorker extends MessageReceiverWorker implements Runnable{
    private static final Logger referenceLogger = LogManager.getLogger("reference_log");
    private final static Logger logger = LogManager.getLogger(ReplicaReceiverWorker.class.getSimpleName());

    private DatabaseAdaper database;

    public ReplicaReceiverWorker(Socket clientSocket, DatabaseAdaper database){
        super(clientSocket);
        this.database = database;
    }

    @Override
    public void run() {
        start();
    }

    @Override
    public void handleJson(JSONObject record, ObjectOutputStream output) {
        if (record.getString("type").equals(MessageType.INSERT_REQUEST)) {
            String alias = record.getString("alias");
            String url = record.getString("url");
            String expirationDate = record.getString("expires");
            // TODO: compose other_nodeID
            referenceLogger.info(String.format("REMOTE_WRITE_RECEIVED(%s,%s)", getClientSocket().getInetAddress(), alias));
            database.insertUrl(alias, url, expirationDate);
            JSONObject response = new JSONObject();
            response.put("type", MessageType.INSERT_CONFIRMATION);
            try {
                output.writeObject(response.toString());
            } catch (IOException e) {
                logger.error(e);
            }
        } else if (record.getString("type").equals(MessageType.CMD_CLOSE)) {
            // Close socket connection to client
            stop();
        }
    }
}
