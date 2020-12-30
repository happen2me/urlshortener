package co.yuanchun.app.cache;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import co.yuanchun.app.AliasRecord;
import co.yuanchun.app.DatabaseAdaper;
import co.yuanchun.app.Node;
import co.yuanchun.app.communication.MessageType;
import co.yuanchun.app.communication.ServerIdentifier;

public class CacheService {
    private static final Logger logger = LogManager.getLogger(ForwardSender.class.getSimpleName());
    
    DatabaseAdaper database;
    Cache<String, String> cache;
    ForwardSender sender;

    public CacheService(DatabaseAdaper database) {
        this.database = database;
        cache = Caffeine.newBuilder().maximumSize(1000).build();
        sender = new ForwardSender();
    }

    /**
     * Returns the value associated with the key in this cache, or null if there is no cached value for the key
     * @param alias alias of the url
     * @return url in String or null
     */
    public String findAliasInCache(String alias) {
        return cache.getIfPresent(alias);
    }

    /**
     * Hash an alias to an integer within a given range
     * @param alias the alias to hash
     * @param range a specific range
     * @return results will in between [0, range-1]
     */
    public int consistentHash(String alias, int range) {
        return alias.hashCode() % range;
    }

    /**
     * Look up an alias in database, and save it to local cash if found.
     * @param alias
     * @return
     */
    public String findAliasInDatabase(String alias) {
        // Look up in database
        AliasRecord record = database.findAliasRecord(alias);
        // Write to local cache
        if (record != null) {
            cache.put(alias, record.getUrl());
            return record.getUrl();
        } else {
            return "";
        }
    }

    /**
     * Connect to a dest server, send alias read request, wait for response, and close connection.
     * @param dest
     * @param alias
     */
    public void forwardQuery(ServerIdentifier dest, String alias) {
        try {
            sender.startConnection(dest);
        } catch (IOException e) {
            logger.error("Can't connect to " + dest + ", please confirm whether the server is still online");
            return;
        }
        JSONObject response = sender.forwardQuery(dest, alias);
        sender.stopConnection();
        if(response.getString("type") == MessageType.READ_FORWARD_CONFIRMATION){
            String url = response.getString("url");
        }
        else if(response.getString("type") == MessageType.READ_FORWARD_NOT_FOUND){
            // TODO: not found
        }
        else{
            
        }
        
    }

    public static void main(String[] args) {
        DatabaseAdaper database;
        try {
            database = new DatabaseAdaper(Node.DB_LOC_PREFIX + ":memory:");
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        database.initializeDb();
        CacheService cacheService = new CacheService(database);

        database.insertUrl("alias1", "1.com", new Timestamp(System.currentTimeMillis()));
        database.insertUrl("alias2", "2.com", new Timestamp(System.currentTimeMillis()));
        // Test load in cache
        System.out.println("find alias1 in cache is null?: " + (cacheService.findAliasInCache("alias1") == null));
        System.out.println("find alias 1 in database: " + cacheService.findAliasInDatabase("alias1"));
        System.out.println("find alias1 again in cache: " + cacheService.findAliasInCache("alias1"));
    }
}
