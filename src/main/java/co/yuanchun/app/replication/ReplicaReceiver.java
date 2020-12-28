package co.yuanchun.app.replication;

import java.net.Socket;

import co.yuanchun.app.DatabaseAdaper;
import co.yuanchun.app.communication.MessageReceiver;

public class ReplicaReceiver extends MessageReceiver implements Runnable {
    // private static final Logger logger = LogManager.getLogger(ReplicaReceiver.class.getSimpleName());

    protected Thread runningThread;
    private DatabaseAdaper database;

    public ReplicaReceiver(int replicatorPort, DatabaseAdaper database){
        super(replicatorPort);
        this.runningThread = null;
        
        this.database = database;
    }

    @Override
    public void run(){
        start();
    }

    @Override
    public void handleClientSocket(Socket clientSocket) {
        new Thread(
                new ReplicaReceiverWorker(clientSocket, database)
            ).start();
    }

    //test
    // public static void main(String[] args) {
    //     int server_port = Integer.parseInt(args[0]);
    //     ReplicaReceiver server = new ReplicaReceiver(server_port, null);
    //     server.run();
    // }
    
}
