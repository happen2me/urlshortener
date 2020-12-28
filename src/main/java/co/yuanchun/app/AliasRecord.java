package co.yuanchun.app;

import java.sql.Timestamp;

import com.opencsv.bean.CsvBindByName;

public class AliasRecord {
    @CsvBindByName
    private String alias;
    @CsvBindByName
    private String url;
    @CsvBindByName
    private Timestamp expires;

    public AliasRecord(String alias, String url, Timestamp expires) {
        this.alias = alias;
        this.url = url;
        this.expires = expires;
    }

    public AliasRecord(String alias, String url, String expires) {
        this.alias = alias;
        this.url = url;
        setExpires(expires);
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Timestamp getExpires() {
        return expires;
    }

    public void setExpires(Timestamp expires) {
        this.expires = expires;
    }

    public void setExpires(String expires){
        this.expires = Timestamp.valueOf(expires);
    }
        
}
