package co.yuanchun.app.communication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public abstract class MessageSender {
    private static final Logger logger = LogManager.getLogger(MessageSender.class.getSimpleName());
    
    private Socket senderSocket;
    private ObjectOutputStream outputStream = null;
    private ObjectInputStream inputStream = null;
    protected ServerIdentifier serverToConnect;

    public MessageSender() {

    }

    public void startConnection(String ip, int port) throws IOException{
        serverToConnect = new ServerIdentifier(ip, port);

        // might throw IOException
        senderSocket = new Socket(ip, port);

        try {
            outputStream = new ObjectOutputStream(senderSocket.getOutputStream());
            inputStream = new ObjectInputStream(senderSocket.getInputStream());
        } catch (IOException e) {
            logger.error("Error when get out/input stream", e);
        }
    }

    public JSONObject sendJson(JSONObject msg) throws IOException{
        JSONObject failure = new JSONObject();
        failure.put("type", MessageType.FAILURE);

        if (outputStream == null) {
            logger.error("Outputstream is null");
            return failure;
        }
        
        outputStream.writeObject(msg.toString());

        Object o = null;
        try {
            logger.debug("reading after sending " + msg.getString("type"));
            o = inputStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if(o instanceof String){
            String jsonString = (String) o;
            JSONObject response = null;
            try {
                response = new JSONObject(jsonString);
            } catch (Exception e) {
                logger.error("Can't parse " + jsonString + " to json object.", e);
                return failure;
            }
            return response;
        }
        else{
            return failure;
        }
    }

    public void stopConnection(){
        if (outputStream == null) {            
            try {
                senderSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.error("Output stream is null, probably connection hasn't been established. Socket closed.");
            return;
        }
        // Inform server to close this connection
        JSONObject msg = new JSONObject();
        msg.put("type", "cmd-close");
        try {
            outputStream.writeObject(msg.toString());
        } catch (IOException e1) {
            logger.error("repsend: error inform server close", e1);
        }

        try {
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            logger.error("Error closing in//output streams");
            e.printStackTrace();
        }
        
        try {
            senderSocket.close();
        } catch (IOException e) {
            logger.error("Error closing sender socket");
            e.printStackTrace();
        }
    }

    public boolean isIOStreamsValid(){
        if(inputStream == null || outputStream==null){
            return false;
        }
        else{
            return true;
        }
    }
}
