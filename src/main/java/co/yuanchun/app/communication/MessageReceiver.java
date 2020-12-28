package co.yuanchun.app.communication;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class MessageReceiver{
    private static final Logger logger = LogManager.getLogger(MessageReceiver.class.getSimpleName());

    protected int serverPort;
    protected ServerSocket serverSocket;
    private boolean isStopped;

    public MessageReceiver(int replicatorPort){
        this.serverPort = replicatorPort;
        this.isStopped = false;
        this.serverSocket = null;
    }

    public abstract void handleClientSocket(Socket clientSocket);

    public void start(){
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
            handleClientSocket(clientSocket);
        }
    }

    // @Override
    // public void run(){
    //     synchronized(this){
    //         this.runningThread = Thread.currentThread();
    //     }
    //     openServerSocket();
    //     while(!isStopped()){
    //         Socket clientSocket = null;
    //         try {
    //             logger.debug("Receiver started");
    //             clientSocket = this.serverSocket.accept();
    //         } catch (IOException e) {
    //             if (isStopped()) {
    //                 logger.debug("Replicator stopped");
    //                 return;
    //             }
    //             throw new RuntimeException("Error accepting client connection", e);
    //         }
    //         new Thread(
    //             new ReceiverWorker(clientSocket, database)
    //         ).start();
    //     }        
    // }


    public synchronized boolean isStopped() {
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

    public void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + this.serverPort, e);
        }
    }

    protected ServerSocket getServerSocket(){
        return serverSocket;
    }
}
