package co.yuanchun.app.replication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReplicaSender {
    private static final Logger logger = LogManager.getLogger(ReplicaReceiver.class.getName());
    
    private Socket senderSocket;
    ObjectOutputStream outputStream = null;
    ObjectInputStream inputStream = null;

    public ReplicaSender() {

    }

    public void startConnection(String ip, int port) {
        try {
            senderSocket = new Socket(ip, port);
        } catch (IOException e) {
            logger.error("Error occured when start connection");
            e.printStackTrace();
            return;
        }
        try {
            outputStream = new ObjectOutputStream(senderSocket.getOutputStream());
            inputStream = new ObjectInputStream(senderSocket.getInputStream());
        } catch (IOException e) {
            logger.error("Error when get out/input stream");
            e.printStackTrace();
        }
    }

    public String sendMessage(String msg) throws IOException {
        if (outputStream == null) {
            throw new IOException("outputstream is null");
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
    public static void main(String[] args) {
        ReplicaSender sender = new ReplicaSender();
        int port_to_connect = Integer.parseInt(args[0]);
        sender.startConnection("localhost", port_to_connect);
        String response = "";
        String response2 = "";
        try {
            response = sender.sendMessage("hello from sender");
            response2 = sender.sendMessage("stop");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("response: " + response + ", "+ response2);
    }
}
