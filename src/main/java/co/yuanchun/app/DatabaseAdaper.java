package co.yuanchun.app;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import com.opencsv.CSVReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseAdaper {
    private final static Logger logger = LogManager.getLogger(DatabaseAdaper.class.getSimpleName());
    private Connection connection;
    private final static String urlTableName = "URL";
    private final static String insertSQL = "INSERT INTO " + urlTableName + "(alias, url, expires) VALUES(?, ?, ?)";
    private PreparedStatement insertQuery;
    private final static String readSQL = "SELECT * FROM " + urlTableName + " WHERE alias=?";
    private PreparedStatement readQuery;
    private final static String deleteSQL = "DELETE FROM " + urlTableName + " WHERE alias=?";
    private PreparedStatement deleteQuery;
    

    public DatabaseAdaper(String dbLocation) throws SQLException {
        this.connection = DriverManager.getConnection(dbLocation);
    }

    synchronized public void initializeDb(){
        try {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.
            String urlSchemaSql = "CREATE TABLE IF NOT EXISTS " + urlTableName +
                " (alias VARCHAR(16) PRIMARY KEY NOT NULL," +
                "url VARCHAR(512) NOT NULL," +
                "expires TIMESTAMP NOT NULL);";
            String dateIndexSql = "CREATE INDEX IF NOT EXISTS expire_idx " +
                "ON " + urlTableName + " (expires)";
            statement.executeUpdate(urlSchemaSql);
            statement.execute(dateIndexSql);
            statement.close();
        } catch (SQLException e) {            
            e.printStackTrace();
        }

        // setup
        try {
            insertQuery = connection.prepareStatement(insertSQL);
            readQuery = connection.prepareStatement(readSQL);
            deleteQuery = connection.prepareStatement(deleteSQL);
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public void bulkloadDataset(String datasetPath) {
        try {
          int aliasCount = connection.createStatement().executeQuery("SELECT COUNT(*) FROM " + urlTableName).getInt(1);
          if (aliasCount == 0) {
            logger.info("Bulkloading dataset");
            try (CSVReader reader = new CSVReader(new FileReader(datasetPath))) {
                reader.readNext();
                String[] line;
                while ((line = reader.readNext()) != null) {
                    String alias = line[0];
                    String expires = line[1];
                    String url = line[2];
                    AliasRecord record = new AliasRecord(alias, url, expires);
                    insertAlias(record);
                }
            } catch (Exception e) {
                logger.error("Can't read from csv file: " + datasetPath, e);
            }
            logger.info("Dataset loaded");
          }
        } catch (SQLException e) {
          logger.error(e.getMessage());
          throw new RuntimeException("Could not load CSV file.");
        }
      }

    /**
     * Insert a url record into database
     * @param alias hashed url
     * @param url original url
     * @param expires expires String, needs to be in the format of "yyyy-MM-dd HH:mm:ss.msmsms"
     */
     public void insertAlias(String alias, String url, String expires){
        insertAlias(alias, url, Timestamp.valueOf(expires));
    }

    /**
     * Insert a url record into database
     * @param alias hashed url
     * @param url original url
     * @param expires expiring time stamp, this is of type java.sql.Timestamp
     */
    synchronized public void insertAlias(String alias, String url, Timestamp expires) {
        try {
            insertQuery.setString(1, alias);
            insertQuery.setString(2, url);
            insertQuery.setTimestamp(3, expires);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            int updatedRecords = insertQuery.executeUpdate();
            if (updatedRecords != 1) {
              logger.error("No record was updated.");
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        logger.debug("Successfully inserted " + alias+" : " + url);
    }

    public void insertAlias(AliasRecord record){
        insertAlias(record.getAlias(), record.getUrl(), record.getExpires());
    }

    /**
     * Look up origianl URL given alias.
     * @param alias
     * @return "" if not found, url string otherwise.
     */
    synchronized public String findAlias(String alias){
        String url = "";
        ResultSet result = lookupAliasInDatabase(alias);
        if (result != null) {
            try {
                url = result.getString("url");
            } catch (SQLException e) {
                logger.error(e);
            }
        }
        return url;
    }

    synchronized public AliasRecord findAliasRecord(String alias){
        String url = "";
        Timestamp expires = null;
        ResultSet result = lookupAliasInDatabase(alias);
        if (result == null) {
            return null;
        }
        else{
            try {
                url = result.getString("url");
                expires = result.getTimestamp("expires");
            } catch (SQLException e) {
                logger.error(e);
            }            
        }
        return new AliasRecord(alias, url, expires);        
    }

    /**
     * Do the actual look up in database
     * @param alias
     * @return
     */
    synchronized private ResultSet lookupAliasInDatabase(String alias){
        ResultSet result = null;
        try {
            readQuery.setString(1, alias);
        } catch (SQLException e1) {
            logger.error(e1.getMessage());
            throw new RuntimeException("Could not prepare read query.");
        }
        try {
            result = readQuery.executeQuery();
            if (result.next()) {
                logger.info("Found url: " + result.getString("url") + " for alias " + alias);
                Timestamp expires = result.getTimestamp("expires");
                if (expires.after(new Timestamp(System.currentTimeMillis()))) {
                    logger.info(alias + " has expired");
                    deleteQuery.setString(1, alias);
                    deleteQuery.executeUpdate();
                }
            }
            else{
                // set result to null if not found
                result = null;
                logger.info("Alias " + alias + " not found");
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        return result;
    }
}
