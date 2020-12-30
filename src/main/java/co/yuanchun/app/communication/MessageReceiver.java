package co.yuanchun.app.communication;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.yuanchun.app.DatabaseAdaper;
import co.yuanchun.app.cache.CacheService;

public class MessageReceiver implements Runnable{
    private static final Logger logger = LogManager.getLogger(MessageReceiver.class.getSimpleName());

    protected int serverPort;
    protected ServerSocket serverSocket;
    private boolean isStopped;

    protected Thread runningThread;
    private DatabaseAdaper database;
    private CacheService cache;

    public MessageReceiver(int replicatorPort, DatabaseAdaper database, CacheService cache){
        this.serverPort = replicatorPort;
        this.database = database;
        this.cache = cache;

        this.isStopped = false;
        this.serverSocket = null;
        this.runningThread = null;
    }

    @Override
    public void run() {
        start();
    }

    public void handleClientSocket(Socket clientSocket) {
        new Thread(
                new MessageReceiverWorker(clientSocket, database, cache)
            ).start();
    }

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
