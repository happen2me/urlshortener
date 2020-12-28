package co.yuanchun.app;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.opencsv.CSVReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseAdaper {
    private static final Logger referenceLogger = LogManager.getLogger("reference_log");
    private final static Logger logger = LogManager.getLogger(DatabaseAdaper.class.getSimpleName());
    private Connection connection;
    private final static String urlTableName = "URL";
    private final static String insertSQL = "INSERT INTO URL(alias, url, expires) VALUES(?, ?, ?)";
    private PreparedStatement insertQuery;
    private final static String readSQL = "SELECT * FROM " + urlTableName + " WHERE alias=?";
    private PreparedStatement readQuery;
    

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
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public void bulkloadDataset(String datasetPath) {
        try {
          int aliasCount = connection.createStatement().executeQuery("SELECT COUNT(*) FROM " + urlTableName).getInt(1);
          if (aliasCount == 0) {
            logger.info("Bulkloading dataset");
            // TODO: use java.sql.Timestamp instead of Calendar
            try (CSVReader reader = new CSVReader(new FileReader(datasetPath))) {
                reader.readNext();
                String[] line;
                while ((line = reader.readNext()) != null) {
                    String alias = line[0];
                    String expires = line[1];
                    String url = line[2];
                    AliasRecord record = new AliasRecord(alias, url, expires);
                    insertUrl(record);
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
     public void insertUrl(String alias, String url, String expires){
        insertUrl(alias, url, Timestamp.valueOf(expires));
    }

    /**
     * Insert a url record into database
     * @param alias hashed url
     * @param url original url
     * @param expires expiring time stamp, this is of type java.sql.Timestamp
     */
    synchronized public void insertUrl(String alias, String url, Timestamp expires) {
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
        logger.debug("Database: successfully inserted " + alias+" : " + url);
    }

    public void insertUrl(AliasRecord record){
        insertUrl(record.getAlias(), record.getUrl(), record.getExpires());
    }

    /**
     * Look up origianl URL given alias.
     * @param alias
     * @return "" if not found, url string otherwise.
     */
    synchronized public String findAlias(String alias){
        String url = "";
        try {
            readQuery.setString(1, alias);
        } catch (SQLException e1) {
            logger.error(e1.getMessage());
            throw new RuntimeException("Could not prepare read query.");
        }
        try {
            ResultSet result = readQuery.executeQuery();
            if (result.next()) {
                url = result.getString("url");
                logger.info("Database: found url: " + url + " for alias " + alias);
            }
            else{
                logger.info("Database: Alias " + alias + " not found");
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }        
        return url;
    }

}
