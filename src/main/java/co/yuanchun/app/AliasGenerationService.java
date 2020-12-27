package co.yuanchun.app;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Calendar;

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
        String alias = generateHash(url);
        String urlFound = database.findAlias(alias);
        while(urlFound != ""){ // modify url if duplicated
            url = increaseUrl(urlFound);
            alias = generateHash(url); // iterate increamentally through all other collisions to find ununsed hash
            urlFound = database.findAlias(alias);
        }
        Calendar expires = generateExpireDateBasedCurrentTime();
        referenceLogger.info(String.format("GENERATED_HASH_FOR_URL(%s,%s)", alias, url));
        return new AliasRecord(alias, url, expires);
    }

    public static Calendar generateExpireDateBasedCurrentTime(){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, VALID_YEARS);
        return calendar;
    }

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

    public static String stripeDuplicateUrl(String url){
        String[] splits = url.split(" ", 2); // url is "No[space]url" or "url"
        if(splits.length > 1){
            url = splits[1];
        }
        return url;
    }

    private String generateHash(String url) {
        byte[] hash = md5.digest(url.getBytes());
        String key = Base64.getEncoder().encodeToString(hash);
        return key.substring(0, ALIAS_LENGTH);
    }

    public DatabaseAdaper getDatabaseAdapter(){
        if (database == null) {
            throw new NullPointerException("Database is not instantiated yet");
        }
        return database;
    }


}
