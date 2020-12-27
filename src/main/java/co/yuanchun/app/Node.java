package co.yuanchun.app;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.yuanchun.app.replication.ReplicationService;
import co.yuanchun.app.replication.ServerIdentifier;

public class Node {
    private static final Logger referenceLogger = LogManager.getLogger("reference_log");
    private static final Logger logger = LogManager.getLogger(AliasGenerationService.class.getSimpleName());

    private static final String DB_LOC_PREFIX = "jdbc:sqlite:";

    private DatabaseAdaper dbAdapter;
    private int replicationListenningPort;
    private List<ServerIdentifier> serverList; // servers to communicate to
    private AliasGenerationService aliasGenerationService;
    private ReplicationService replicationService;
    

    /**
     * Constructor of Node, each node is an abstract of a single physical server.
     * Please call initializeServices() after construction
     * @param databasePath
     * @param serverList
     * @param replicationListenningPort
     */
    public Node(String databasePath, List<ServerIdentifier> serverList, int replicationListenningPort){
        this.serverList = serverList;
        this.replicationListenningPort = replicationListenningPort;
        setupDatabase(databasePath);
    }

    public void initializeServices(){
        // initilize alias generation service
        aliasGenerationService = new AliasGenerationService(dbAdapter);
        // initialize replication service
        replicationService = new ReplicationService(dbAdapter, serverList);
        replicationService.startListen(replicationListenningPort);
    }

    /**
     * Generate alias for a url and save it to both local and remote database.
     * This is called by client connection handler
     * @param url
     * @return generated alias
     */
    public String addUrl(String url){
        // generate alias with AliasGenerationService
        AliasRecord record = aliasGenerationService.generateAlias(url);
        // save to local database
        dbAdapter.insertUrl(record.getAlias(), record.getUrl(), record.getExpires());
        referenceLogger.info(String.format("LOCAL_WRITE(%s) ", record.getAlias()));
        // replciate to remote database once it's locally saved
        // TODO: make it unblock
        replicationService.propagateAlias(record.getAlias(), record.getUrl(), record.getExpires());
        return record.getAlias();
    }

    public String findAlias(String alias){
        // look up an alias from database
        String urlFound = dbAdapter.findAlias(alias);
        // alias generation service defined how to handle duplicated url,
        // therefore it should also be responsible for analyzed transformed url
        String url = AliasGenerationService.stripeDuplicateUrl(urlFound);
        return url;
    }

    private void setupDatabase(String databasePath){
        try {
            dbAdapter = new DatabaseAdaper(DB_LOC_PREFIX + databasePath);
        } catch (SQLException e) {
            logger.error("Failed to connect to data base.", e);
            logger.error("Aborted..");
            System.exit(1);
        }
        dbAdapter.initializeDb();
    }
}
