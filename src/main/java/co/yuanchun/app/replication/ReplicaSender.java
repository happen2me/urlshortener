package co.yuanchun.app.replication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class ReplicaSender {
    private static final Logger referenceLogger = LogManager.getLogger("reference_log");
    private static final Logger logger = LogManager.getLogger(ReplicaReceiver.class.getSimpleName());
    
    private Socket senderSocket;
    ObjectOutputStream outputStream = null;
    ObjectInputStream inputStream = null;
    ServerIdentifier serverToConnect;

    public ReplicaSender() {

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

    public boolean sendMessage(String alias, String url, Timestamp expireDate) throws IOException{
        if (outputStream == null) {
            logger.error("Outputstream is null");
            return false;
        }

        JSONObject msg = new JSONObject();
        msg.put("type", MessageType.INSERT_REQUEST);
        //msg.put("from", value)
        msg.put("alias", alias);
        msg.put("url", url);
        msg.put("expires", expireDate.toString());
        
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
                return false;
            }
            if (response.getString("type").equals(MessageType.INSERT_CONFIRMATION)){
                referenceLogger.info(String.format(" REMOTE_WRITE_CONFIRMED(%s,%s) ", serverToConnect, alias));
                return true;
            }
            else{
                return false;
            }
        }
        else{
            return false;
        }
    }

    @Deprecated
    public String sendMessage(String msg) throws IOException {
        if (outputStream == null) {
            logger.error("outputstream is null");
            return "";
        }
        outputStream.writeObject(msg);
        Object o = null;
        try {
            logger.debug("reading after sending " + msg);
            o = inputStream.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if(o instanceof String){
            return (String) o;
        }
        else{
            return "Empty response";
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

    // Test
    // public static void main(String[] args) {
    //     ReplicaSender sender = new ReplicaSender();
    //     int port_to_connect = Integer.parseInt(args[0]);
    //     sender.startConnection("localhost", port_to_connect);
    //     String response = "";
    //     String response2 = "";
    //     try {
    //         response = sender.sendMessage("hello from sender");
    //         response2 = sender.sendMessage("stop");
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    //     System.out.println("response: " + response + ", "+ response2);
    // }
}
