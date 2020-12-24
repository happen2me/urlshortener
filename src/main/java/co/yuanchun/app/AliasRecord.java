package co.yuanchun.app;

import java.util.Calendar;

public class AliasRecord {
    private String alias;
    private String url;
    private Calendar expires;

    public AliasRecord(String alias, String url, Calendar expires) {
        this.alias = alias;
        this.url = url;
        this.expires = expires;
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

    public Calendar getExpires() {
        return expires;
    }

    public void setExpires(Calendar expires) {
        this.expires = expires;
    }
        
}
