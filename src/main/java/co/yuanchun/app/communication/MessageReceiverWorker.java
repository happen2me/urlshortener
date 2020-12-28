package co.yuanchun.app.communication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import co.yuanchun.app.DatabaseAdaper;
import co.yuanchun.app.communication.MessageType;

public abstract class MessageReceiverWorker {
    private final static Logger logger = LogManager.getLogger(MessageReceiverWorker.class.getSimpleName());

    private Socket clientSocket = null;
    private boolean isStopped;

    public MessageReceiverWorker(Socket clientSocket){
        this.clientSocket = clientSocket;
        this.isStopped = false;
    }


    protected abstract void handleJson(JSONObject record, ObjectOutputStream outputStream);
   
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
