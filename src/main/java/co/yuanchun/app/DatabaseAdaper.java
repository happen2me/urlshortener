package co.yuanchun.app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Nullable;

public class DatabaseAdaper {
    private Connection connection;
    private String urlTableName;
    private String userTableName;

    public DatabaseAdaper(String dbLocation) throws SQLException {
        this.connection = DriverManager.getConnection(dbLocation);
        this.urlTableName = "URL";
        this.userTableName = "User";
    }

    public void initializeDb(){        
        try {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.
            String urlSchemaSql = "CREATE TABLE IF NOT EXISTS " + urlTableName +
                " (Hash VARCHAR(16) PRIMARY KEY NOT NULL," +
                "OriginalURL VARCHAR(512) NOT NULL," + 
                "CreationDate DATETIME NOT NULL," + 
                "ExpirationDate DATETIME NOT NULL," + 
                "UserID INT);";
            String userSchemaSql = "CREATE TABLE IF NOT EXISTS " + userTableName +
                " (UserID INT PRIMARY KEY NOT NULL," +
                "Name VARCHAR(20)," + 
                "Email VARCHAR(32)," + 
                "CreationDate DATETIME," +
                "LastLogin DATETIME);";
            statement.executeUpdate(urlSchemaSql);
            statement.execute(userSchemaSql);
            statement.close();
        } catch (SQLException e) {            
            e.printStackTrace();
        }
    }

    public void insertUrl(String alias, String url, Date creationDate, 
        @Nullable Date expirationDate, @Nullable Integer userID) {
        try {
            Statement statement = connection.createStatement();
            String insertSql = "INSERT INTO " + urlTableName +
                "(Hash, OriginalURL, CreationDate, ExpirationDate, UserID) " +
                String.format("VALUES('%s', '%s', '%s', '%s', %s);", alias, url, toSqlDate(creationDate), 
                    expirationDate==null ? "NULL" : toSqlDate(expirationDate),
                    userID==null ? "NULL" : userID);
                statement.executeUpdate(insertSql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String toSqlDate(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    /**
     * Look up origianl URL given alias.
     * @param alias
     * @return "" if not found, url string otherwise.
     */
    public String findAlias(String alias){
        String url = "";
        try {
            Statement statement = connection.createStatement();
            String querySql = "SELECT OriginalURL " +
            "FROM " + urlTableName + " " +
            "WHERE Hash = '" + alias + "';";
            ResultSet result = statement.executeQuery(querySql);
            while (result.next()) {
                url = result.getString("OriginalURL");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return url;
    }

}
