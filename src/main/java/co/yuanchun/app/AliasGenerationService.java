package co.yuanchun.app;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Base64;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Encapsulates main operations of shortening URL, this inclues
 * generating, inserting, finding url:alias pairs and collision
 * detection.
 */
public class AliasGenerationService {
    private static final Logger referenceLogger = LogManager.getLogger("reference_log");
    private static final Logger logger = LogManager.getLogger(AliasGenerationService.class.getSimpleName());

    final static String DB_LOC_PREFIX = "jdbc:sqlite:";
    final static int ALIAS_LENGTH = 6;
    final static String HASH_METHOD = "MD5";
    final static int VALID_YEARS = 5;
    private static final int EXPIRES_AFTER = 1000 * 60 * 60 * 24 * 365 * VALID_YEARS;
    DatabaseAdaper database = null;
    MessageDigest md5;

    public AliasGenerationService(DatabaseAdaper databaseAdaper) {
        this.database = databaseAdaper;
        try {
            md5 = MessageDigest.getInstance(HASH_METHOD);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Needs" + HASH_METHOD + " to be installed.");
        }
    }

    @Deprecated
    public String insertUrl(String url) {
        AliasRecord record = generateAlias(url);
        database.insertUrl(record.getAlias(), record.getUrl(), record.getExpires());
        return record.getAlias();
    }

    public AliasRecord generateAlias(String url){
        String uniqueUrl = url;
        String alias = generateHash(uniqueUrl);
        // replace / to = if it starts with /
        if (alias.startsWith("/")) {
            alias = "=" + alias.substring(1);
        }
        String urlFound = database.findAlias(alias);
        int unique = -1;
        while(urlFound != ""){ // modify url if duplicated
            //uniqueUrl = increaseUrl(uniqueUrl);
            uniqueUrl = url + unique;
            alias = generateHash(uniqueUrl); // iterate increamentally through all other collisions to find ununsed hash
            urlFound = database.findAlias(alias);
            unique++;
        }
        Timestamp expires = generateExpireDateBasedCurrentTime();
        return new AliasRecord(alias, url, expires);
    }

    public static Timestamp generateExpireDateBasedCurrentTime(){
        Timestamp expires = new Timestamp(System.currentTimeMillis() + EXPIRES_AFTER);
        return expires;
    }

    @Deprecated
    private String increaseUrl(String url){
        String[] splits = url.split(" ", 2); // split by first space
        if(splits.length > 1){ // has leading numbers, namely at least second collision
            int curNo = 0;
            try {
                curNo = Integer.parseInt(splits[0]);
            } catch (Exception e) {
                logger.error("Failed to parse url: " + url + " from database.");
            }
            url = Integer.toString(curNo+1) + " " + splits[1];                
        }
        else{ // has no leading numbers, namely the first collision
            url = Integer.toString(0) + " " + url;
        }
        logger.debug("concatenated url: " + url);
        return url;
    }

    @Deprecated
    public static String stripeDuplicateUrl(String url){
        // String[] splits = url.split(" ", 2); // url is "No[space]url" or "url"
        // if(splits.length > 1){
        //     url = splits[1];
        // }
        return url;
    }

    private String generateHash(String url) {
        byte[] hash = md5.digest(url.getBytes());
        String key = Base64.getEncoder().encodeToString(hash);
        referenceLogger.info(String.format("GENERATED_HASH_FOR_URL(%s,%s)", key, url));
        return key.substring(0, ALIAS_LENGTH);
    }

    public DatabaseAdaper getDatabaseAdapter(){
        if (database == null) {
            throw new NullPointerException("Database is not instantiated yet");
        }
        return database;
    }


}
