package co.yuanchun.app.replication;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.yuanchun.app.DatabaseAdaper;

public class ReplicationService {
    private static final Logger logger = LogManager.getLogger(ReplicationService.class.getName());

    private DatabaseAdaper database;
    private List<ServerIdentifier> serverList;
    private Thread receiverThread;
    private ReplicaSender replicaSender;

    /**
     * To write to remote database
     * 
     * @param database   local database instance; it is used to save replications
     *                   from remote nodes
     * @param serverList is a list of remote servers to send messages to, each
     *                   component is of the type ServerIdentifier
     */
    public ReplicationService(DatabaseAdaper database, List<ServerIdentifier> serverList) {
        this.database = database;
        this.serverList = serverList;
        this.replicaSender = new ReplicaSender();
    }

    public void startListen(int port) {
        logger.info("Start listening incoming replication on port " + port);
        receiverThread = new Thread(new ReplicaReceiver(port, database));
        receiverThread.start();
    }

    public void stopListen() {
        // TODO: implement asyncronous stop method
    }

    public void addAlias(String alias, String url, Calendar expires) {
        for (ServerIdentifier serverIdentifier : serverList) {
            String ip = serverIdentifier.getIp();
            int port = serverIdentifier.getPort();
            replicaSender.startConnection(ip, port);
            try {
                replicaSender.sendMessage(alias, url, expires);
            } catch (IOException e) {
                
            }
            replicaSender.stopConnection();
        }
    }    
}
