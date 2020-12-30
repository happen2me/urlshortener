package co.yuanchun.app;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.yuanchun.app.replication.ReplicationService;
import co.yuanchun.app.cache.CacheService;
import co.yuanchun.app.communication.MessageService;
import co.yuanchun.app.communication.ServerIdentifier;

public class Node {
    private static final Logger referenceLogger = LogManager.getLogger("reference_log");
    private static final Logger logger = LogManager.getLogger(AliasGenerationService.class.getSimpleName());

    public static final String DB_LOC_PREFIX = "jdbc:sqlite:";

    private DatabaseAdaper database;
    private int replicationListenningPort;
    private List<ServerIdentifier> serverList; // servers to communicate to
    private AliasGenerationService aliasGenerationService;
    private CacheService cacheService;
    private ReplicationService replicationService;
    private MessageService messageService;
    private ServerIdentifier self;

    /**
     * Constructor of Node, each node is an abstract of a single physical server.
     * Please call initializeServices() after construction
     * 
     * @param databasePath
     * @param serverList
     * @param replicationListenningPort
     */
    public Node(String databasePath, List<ServerIdentifier> serverList, int replicationListenningPort) {
        this.serverList = serverList;
        this.replicationListenningPort = replicationListenningPort;
        self = App.GetIdentifier();
        setupDatabase(databasePath);
    }

    public void initializeServices() {
        // initilize alias generation service
        aliasGenerationService = new AliasGenerationService(database);
        // initialize replication service
        replicationService = new ReplicationService(serverList);
        cacheService = new CacheService(database, serverList);
        messageService = new MessageService(database, cacheService);
        messageService.startListen(replicationListenningPort);
    }

    /**
     * Generate alias for a url and save it to both local and remote database. This
     * is called by client connection handler
     * 
     * @param url
     * @return generated alias
     */
    public String addUrl(String url) {
        // generate alias with AliasGenerationService
        AliasRecord record = aliasGenerationService.generateAlias(url);
        // save to local database
        database.insertUrl(record.getAlias(), record.getUrl(), record.getExpires());
        referenceLogger.info(String.format("LOCAL_WRITE(%s) ", record.getAlias()));
        // replciate to remote database once it's locally saved
        // TODO: make it unblock
        replicationService.propagateAlias(record.getAlias(), record.getUrl(), record.getExpires());
        return record.getAlias();
    }

    public String findAlias(String alias) {
        // compute node that is responsible for this alias
        ServerIdentifier dest = cacheService.consistentHash(alias);

        String url; // result

        // if the node is this server, look it up from local cache,
        // if not in local cache, load it from database and save to cache
        if(dest.equals(self)){
            url = cacheService.findAliasInCache(alias);
            if(url == null){
                url = cacheService.findAliasInDatabase(alias);
            }
        }
        else{ // if the node is not this server, forward the request to responsible server
           url = cacheService.forwardQuery(dest, alias);
        }

        return url;
    }

    private void setupDatabase(String databasePath) {
        try {
            database = new DatabaseAdaper(DB_LOC_PREFIX + databasePath);
        } catch (SQLException e) {
            logger.error("Failed to connect to data base.", e);
            logger.error("Aborted..");
            System.exit(1);
        }
        database.initializeDb();
        if (App.GetGloablVarsMap().containsKey("initial_dataset")) {
            logger.info("Bulk loading data set from " + App.GetGlobalVar("initial_dataset") );
            database.bulkloadDataset(App.GetGlobalVar("initial_dataset"));
        }
    }
}
