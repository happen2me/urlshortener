package co.yuanchun.app.replication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import co.yuanchun.app.DatabaseAdaper;
import co.yuanchun.app.AliasGenerationService;

public class ReceiverWorker implements Runnable{
    private static final Logger referenceLogger = LogManager.getLogger("reference_log");
    private final static Logger logger = LogManager.getLogger(ReceiverWorker.class.getSimpleName());

    private Socket clientSocket = null;
    private boolean isStopped;
    private DatabaseAdaper database;

    public ReceiverWorker(Socket clientSocket, DatabaseAdaper database){
        this.clientSocket = clientSocket;
        this.isStopped = false;
        this.database = database;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
            while (!isStopped()) {
                Object o = null;
                try {
                    o = input.readObject();
                } catch (ClassNotFoundException e) {
                    logger.error("RcvWrk: can't read object from stream. ", e);
                }
                // TODO: convert to bytes operation
                if(o instanceof String){
                    String jsonString = (String) o;
                    JSONObject record = null;                
                    try {
                        record = new JSONObject(jsonString);
                    } catch (Exception e) {
                        logger.error("Can't parse string" + jsonString + " to json object", e);
                    }
                    if (record.getString("type").equals(MessageType.INSERT_REQUEST)) {
                        String alias = record.getString("alias");
                        String url = record.getString("url");
                        String expirationDate = record.getString("expires");
                        database.insertUrl(alias, url, expirationDate);
                        JSONObject response = new JSONObject();
                        response.put("type", MessageType.INSERT_CONFIRMATION);
                        output.writeObject(response.toString());
                    }
                    else if(record.getString("type").equals(MessageType.CMD_CLOSE)){ 
                        // Close socket connection to client
                        stop();
                    }
                }
                else{
                    logger.error("Got unexpected object");
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.clientSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }
}
