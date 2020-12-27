package co.yuanchun.app.replication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.yuanchun.app.DatabaseAdaper;

public class ReplicationService {
    private static final Logger referenceLogger = LogManager.getLogger("reference_log");
    private static final Logger logger = LogManager.getLogger(ReplicationService.class.getSimpleName());

    private DatabaseAdaper database;
    private List<ServerIdentifier> serverList;
    private Thread receiverThread;
    private ReplicaSender replicaSender;
    private ServerIdentifier selfIdentifier;

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

    /**
     * Propagate an alias record to all other servers.
     * This method will block until it gets response from all other
     * servers.
     * @param alias
     * @param url
     * @param expires
     * @return whether replciations to all servers succeeded
     */
    public boolean propagateAlias(String alias, String url, Calendar expires) {
        List<FutureTask<Boolean>> tasks = new ArrayList<>();
        boolean allSucceeded = true;
        for (ServerIdentifier serverIdentifier : serverList) {
            // FutureTask<Boolean> t = new FutureTask<>(new PropagateTask(serverIdentifier, alias, url, expires));
            // tasks.add(t);
            String ip = serverIdentifier.getIp();
            int port = serverIdentifier.getPort();
            replicaSender.startConnection(ip, port);
            boolean succeeded = false;
            try {
                succeeded = replicaSender.sendMessage(alias, url, expires);
                referenceLogger.info(String.format("REMOTE_WRITE_REQUESTED(%s,%s)", serverIdentifier, alias));
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            replicaSender.stopConnection();
            if (!succeeded) {
                allSucceeded = false;
            }
        }
        // boolean allSucceeded = true;
        // for (FutureTask<Boolean> futureTask : tasks) {
        //     boolean replicateSucceeed;
        //     try {
        //         replicateSucceeed = futureTask.get();
        //         if (!replicateSucceeed) {
        //             allSucceeded = false;
        //         }
        //     } catch (InterruptedException | ExecutionException e) {
        //         allSucceeded = false;
        //         logger.error("Error checking replication result", e);
        //     }            
        // }
        return allSucceeded;
    }

    private class PropagateTask implements Callable<Boolean>{
        private ServerIdentifier serverIdentifier;
        private String alias;
        private String url;
        private Calendar expires;

        public PropagateTask(ServerIdentifier serverIdentifier, String alias, String url, Calendar expires) {
            this.serverIdentifier = serverIdentifier;
            this.alias = alias;
            this.url = url;
            this.expires = expires;
        }

        @Override
        public Boolean call() throws Exception {
            String ip = serverIdentifier.getIp();
            int port = serverIdentifier.getPort();
            replicaSender.startConnection(ip, port);
            boolean succeeded = false;
            try {
                succeeded = replicaSender.sendMessage(alias, url, expires);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            replicaSender.stopConnection();
            return succeeded;           
        }      
    }
}
