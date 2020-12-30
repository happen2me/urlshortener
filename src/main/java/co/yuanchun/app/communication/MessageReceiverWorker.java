package co.yuanchun.app.communication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import co.yuanchun.app.DatabaseAdaper;
import co.yuanchun.app.cache.CacheService;

public class MessageReceiverWorker implements Runnable{
    private static final Logger referenceLogger = LogManager.getLogger("reference_log");
    private final static Logger logger = LogManager.getLogger(MessageReceiverWorker.class.getSimpleName());

    private Socket clientSocket = null;
    private boolean isStopped;

    private DatabaseAdaper database;
    private CacheService cache;

    public MessageReceiverWorker(Socket clientSocket, DatabaseAdaper database, CacheService cache){
        this.clientSocket = clientSocket;
        this.isStopped = false;
        this.database = database;
        this.cache = cache;
    }

    @Override
    public void run() {
        start();
    }

    public void handleJson(JSONObject json, ObjectOutputStream output) {
        if (json.getString("type").equals(MessageType.INSERT_REQUEST)) {
            processIncomingReplication(json, output);
        } 
        else if(json.getString("type").equals(MessageType.READ_FORWARD_REQUEST)){
            processForwardedRead(json, output);
        }
        else if (json.getString("type").equals(MessageType.CMD_CLOSE)) {
            // Close socket connection to client
            stop();
        }
    }

    private void processIncomingReplication(JSONObject record, ObjectOutputStream output){
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
    }

    private void processForwardedRead(JSONObject readParams, ObjectOutputStream output){
        String alias = "";
        try {
            alias = readParams.getString("alias");
        } catch (JSONException e) {
            logger.error("Forwarded read request should have a parameter of alias, but not found", e);
            return;
        }
        referenceLogger.info(String.format("FORWARDED_READ_RECEIVED(<other_nodeID>,%s)", alias));
        JSONObject response = new JSONObject();
        String url = null;
        // First look up the alias in cache
        url = cache.findAliasInCache(alias);
        if (url == null) {
            // If the alias is not in cache, look up it from database and save it to local cache
            url = cache.findAliasInDatabase(alias);
            if (url == "") { // url is not in database either
                response.put("type", MessageType.READ_FORWARD_NOT_FOUND);
            }
            else{
                referenceLogger.info(String.format("READ_STORAGE_SUCESS(%s)", alias));
            }
        }
        else{
            referenceLogger.info(String.format("READ_CACHE_SUCESS(%s)", alias));
        }
        if (url != "") {
            response.put("type", MessageType.READ_FORWARD_CONFIRMATION);
            response.put("url", url);
            response.put("alias", alias);
        }
        try {
            output.writeObject(response.toString());
        } catch (IOException e) {
            logger.error(e);
        }
    }
   
    public void start() {
        try {
            ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
            logger.debug("CLIENT SOCKET: " + clientSocket.toString());
            while (!isStopped()) {
                Object o = null;
                try {
                    o = input.readObject();
                } catch (ClassNotFoundException e) {
                    logger.error("can't read object from stream. ", e);
                }
                // TODO: convert to bytes operation
                if(o instanceof String){
                    String jsonString = (String) o;
                    JSONObject json = null;                
                    try {
                        json = new JSONObject(jsonString);
                    } catch (Exception e) {
                        logger.error("Can't parse string" + jsonString + " to json object", e);
                    }
                    handleJson(json, output);
                }
                else{
                    logger.error("Got unexpected object");
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

    }

    public synchronized boolean isStopped() {
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

    protected Socket getClientSocket(){
        return clientSocket;
    }
}
