package co.yuanchun.app;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
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
    private static final Logger logger = LogManager.getLogger(AliasGenerationService.class.getName());

    final static String DB_LOC_PREFIX = "jdbc:sqlite:";
    final static int ALIAS_LENGTH = 6;
    final static String HASH_METHOD = "MD5";
    final static int VALID_YEARS = 5;
    DatabaseAdaper dbAdapter = null;

    public AliasGenerationService(DatabaseAdaper databaseAdaper) {
        this.dbAdapter = databaseAdaper;
    }

    @Deprecated
    public String insertUrl(String url) {
        String alias = generateAlias(url);
        Calendar expires = generateExpireDateBasedCurrentTime();
        dbAdapter.insertUrl(alias, url, expires);
        return alias;
    }

    public String generateAlias(String url){
        String alias = generateHash(url);
        String urlFound = dbAdapter.findAlias(alias);
        while(urlFound != ""){ // modify url if duplicated
            url = increaseUrl(urlFound);
            alias = generateHash(url); // iterate increamentally through all other collisions to find ununsed hash
            urlFound = dbAdapter.findAlias(alias);
        }
        return alias;
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
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance(HASH_METHOD);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Hash method " + HASH_METHOD + " not found.");
            e.printStackTrace();
            return "";
        }
        byte[] hash = md5.digest(url.getBytes());
        String key = Base64.getEncoder().encodeToString(hash);
        return key.substring(0, ALIAS_LENGTH);
    }

    public DatabaseAdaper getDatabaseAdapter(){
        if (dbAdapter == null) {
            throw new NullPointerException("Database is not instantiated yet");
        }
        return dbAdapter;
    }


}
