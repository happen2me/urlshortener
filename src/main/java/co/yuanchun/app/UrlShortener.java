package co.yuanchun.app;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

public class UrlShortener {
    final String DB_LOC = "jdbc:sqlite:sample.db";
    final int ALIAS_LENGTH = 6;
    final String HASH_METHOD = "MD5";
    final int VALID_YEARS = 5;
    DatabaseAdaper dbAdapter;

    public UrlShortener() {
        try {
            dbAdapter = new DatabaseAdaper(DB_LOC);
        } catch (SQLException e) {
            System.err.println("Failed to connect to data base.");
            e.printStackTrace();
        }
        dbAdapter.initializeDb();
    }

    public String insertUrl(String url, Integer userID) {
        String alias = generateAlias(url);
        String urlFound = dbAdapter.findAlias(alias);
        if(urlFound != ""){ // modify url if duplicated
            url = increaseUrl(urlFound);
            return insertUrl(url, userID); // iterate increamentally through all other collisions to find ununsed hash
        }
        Calendar calendar = Calendar.getInstance();
        Date creationDate = calendar.getTime();
        calendar.add(Calendar.YEAR, VALID_YEARS);
        Date expirationDate = calendar.getTime();
        dbAdapter.insertUrl(alias, url, creationDate, expirationDate, userID);
        return alias;
    }

    private String increaseUrl(String url){
        String[] splits = url.split(" ", 2); // split by first space
        if(splits.length > 1){ // has leading numbers, namely at least second collision
            int curNo = 0;
            try {
                curNo = Integer.parseInt(splits[0]);
            } catch (Exception e) {
                System.err.println("Failed to parse url: " + url + " from database.");
            }
            url = Integer.toString(curNo+1) + " " + splits[1];                
        }
        else{ // has no leading numbers, namely the first collision
            url = Integer.toString(0) + " " + url;
        }
        System.out.println("concatenated url: " + url);
        return url;
    }

    private String stripeDuplicateUrl(String url){
        String[] splits = url.split(" ", 2); // url is "No[space]url" or "url"
        if(splits.length > 1){
            url = splits[1];
        }
        return url;
    }

    public String findAlias(String alias){
        String urlFound = dbAdapter.findAlias(alias);
        String url = stripeDuplicateUrl(urlFound);
        return url;
    }

    private String generateAlias(String url) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance(HASH_METHOD);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Hash method " + HASH_METHOD + " not found.");
            e.printStackTrace();
            return "";
        }
        byte[] hash = md5.digest(url.getBytes());
        String key = Base64.getEncoder().encodeToString(hash);
        return key.substring(0, ALIAS_LENGTH);
    }


}