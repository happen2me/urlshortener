package co.yuanchun.app.replication;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.yuanchun.app.DatabaseAdaper;

public class ReplicaReceiver implements Runnable {
    private static final Logger logger = LogManager.getLogger(ReplicaReceiver.class.getSimpleName());

    private int serverPort;
    private ServerSocket serverSocket;
    private boolean isStopped;
    protected Thread runningThread;
    private DatabaseAdaper database;

    public ReplicaReceiver(int replicatorPort, DatabaseAdaper database){
        this.serverPort = replicatorPort;
        this.isStopped = false;
        this.serverSocket = null;
        this.runningThread = null;
        this.database = database;
    }

    @Override
    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while(!isStopped()){
            Socket clientSocket = null;
            try {
                logger.debug("Receiver started");
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if (isStopped()) {
                    logger.debug("Replicator stopped");
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }
            new Thread(
                new ReceiverWorker(clientSocket, database)
            ).start();
        }

        
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + this.serverPort, e);
        }
    }

    //test
    // public static void main(String[] args) {
    //     int server_port = Integer.parseInt(args[0]);
    //     ReplicaReceiver server = new ReplicaReceiver(server_port, null);
    //     server.run();
    // }
    
}
