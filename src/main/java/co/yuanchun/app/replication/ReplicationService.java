package co.yuanchun.app.replication;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.yuanchun.app.communication.ServerIdentifier;

public class ReplicationService {
    private static final Logger referenceLogger = LogManager.getLogger("reference_log");
    private static final Logger logger = LogManager.getLogger(ReplicationService.class.getSimpleName());

    
    private List<ServerIdentifier> serverList;
    private HashMap<ServerIdentifier, ReplicaSender> senders;

    /**
     * To write to remote database
     * 
     * @param database   local database instance; it is used to save replications
     *                   from remote nodes
     * @param serverList is a list of remote servers to send messages to, each
     *                   component is of the type ServerIdentifier
     */
    public ReplicationService(List<ServerIdentifier> serverList) {
        this.serverList = serverList;
        this.senders = new HashMap<>();
        //this.replicaSender = new ReplicaSender();
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
    public boolean propagateAlias(String alias, String url, Timestamp expires) {
        List<FutureTask<Boolean>> tasks = new ArrayList<>();
        boolean allSucceeded = true;
        for (ServerIdentifier serverIdentifier : serverList) {
            // FutureTask<Boolean> t = new FutureTask<>(new PropagateTask(serverIdentifier, alias, url, expires));
            // tasks.add(t);
            
            // start connection if the socket has never been established
            if(!senders.containsKey(serverIdentifier)){
                ReplicaSender sender = new ReplicaSender();   
                try {
                    sender.startConnection(serverIdentifier);
                    sender.keepConnectionAlive(true);
                } catch (IOException e) {
                    logger.error("Can't connect to " + serverIdentifier);
                }
                senders.put(serverIdentifier, sender);
            }

            // start connection if the connection hasn't been started
            // or has been interrupted
            ReplicaSender replicaSender = senders.get(serverIdentifier);
            if (replicaSender.isClosed()) {
                logger.info("Reopenning socket to " + serverIdentifier);
                try {
                    replicaSender.startConnection(serverIdentifier);
                    replicaSender.keepConnectionAlive(true);
                } catch (IOException e) {
                    logger.error("Can't connect to " + serverIdentifier);
                    continue;
                }
            }

            // String ip = serverIdentifier.getIp();
            // int port = serverIdentifier.getPort();
            // try {
            //     replicaSender.startConnection(ip, port);
            // } catch (Exception e) {
            //     logger.error("Can't connect to " + serverIdentifier + ",  please confirm whether it is online");
            //     allSucceeded = false;
            //     continue;
            // }
            
            boolean succeeded = false;
            try {
                succeeded = replicaSender.sendMessage(alias, url, expires);
                referenceLogger.info(String.format("REMOTE_WRITE_REQUESTED(%s,%s)", serverIdentifier, alias));
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
            // keep connection alive
            //replicaSender.stopConnection();
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

    public void stop(){
        for (Map.Entry<ServerIdentifier, ReplicaSender> entry : senders.entrySet()) {
            ReplicaSender sender = entry.getValue();
            sender.stopConnection();
        }
    }

    private class PropagateTask implements Callable<Boolean>{
        private ServerIdentifier serverIdentifier;
        private String alias;
        private String url;
        private Timestamp expires;

        public PropagateTask(ServerIdentifier serverIdentifier, String alias, String url, Timestamp expires) {
            this.serverIdentifier = serverIdentifier;
            this.alias = alias;
            this.url = url;
            this.expires = expires;
        }

        @Override
        public Boolean call() throws Exception {
            // String ip = serverIdentifier.getIp();
            // int port = serverIdentifier.getPort();
            // replicaSender.startConnection(ip, port);
            // boolean succeeded = false;
            // try {
            //     succeeded = replicaSender.sendMessage(alias, url, expires);
            // } catch (IOException e) {
            //     logger.error(e.getMessage());
            // }
            // replicaSender.stopConnection();
            // return succeeded;
            return false;
        }
    }
}
