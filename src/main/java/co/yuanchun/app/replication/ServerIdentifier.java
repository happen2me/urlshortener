package co.yuanchun.app.replication;

public class ServerIdentifier {
    private String ip;
    private int port;

    public ServerIdentifier(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
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
        if ((this.ip == null) ? (other.ip != null) : !this.ip.equals(other.ip)) {
            return false;
        }

        if (this.port != other.port) {
            return false;
        }

        return true;
    }

    @Override
    public String toString(){
        return ip + port;
    }
    
}
