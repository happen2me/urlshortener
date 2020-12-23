package co.yuanchun.app.replication;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import co.yuanchun.app.DatabaseAdaper;
import co.yuanchun.app.UrlShortener;

public class ReceiverWorker implements Runnable{
    private static final Logger referenceLogger = LogManager.getLogger("reference_log");
    private final static Logger logger = LogManager.getLogger(ReceiverWorker.class.getName());

    private Socket clientSocket = null;
    private boolean isStopped;
    // private DatabaseAdaper database = null;

    public ReceiverWorker(Socket clientSocket){
        this.clientSocket = clientSocket;
        this.isStopped = false;
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
                if(o instanceof String){
                    String msg = (String) o;
                    if(msg.equals("stop")){
                        output.writeObject("stopped");
                        stop();
                        logger.debug("worker rcver stopped");
                    }
                    else{
                        System.out.println("saving msg: " + msg);
                        output.writeObject("hello client");;
                    }
                }
                // else if(o instanceof JSONObject){
                //     // TODO: save to database
                //     logger.debug("rceived JSON");
                // }
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
