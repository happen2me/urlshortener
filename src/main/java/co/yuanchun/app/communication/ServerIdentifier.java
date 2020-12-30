package co.yuanchun.app.communication;

public class ServerIdentifier implements Comparable<ServerIdentifier>{
    private String ip;
    private int port;

    public ServerIdentifier(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        // translate localhost to 127.0.0.1 to guanratee comparability
        if (ip == "localhost") {
            return "127.0.0.1";
        }
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object obj){
        if (obj == null) {
            return false;
        }

        if (obj.getClass() != this.getClass()) {
            return false;
        }

        final ServerIdentifier other = (ServerIdentifier) obj;

        
        // use getIp() to leverage localhost translation
        if ((this.ip == null) ? (other.ip != null) : !this.getIp().equals(other.getIp())) {
            return false;
        }

        if (this.port != other.port) {
            return false;
        }

        return true;
    }

    @Override
    public String toString(){
        return getIp() + ":" + port;
    }

    @Override
    public int compareTo(ServerIdentifier o) {
        return this.toString().compareTo(o.toString());
    }
    
}
