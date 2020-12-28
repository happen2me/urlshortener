package co.yuanchun.app.replication;

import java.io.IOException;
import java.sql.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import co.yuanchun.app.communication.MessageSender;
import co.yuanchun.app.communication.MessageType;

public class ReplicaSender extends MessageSender{
    private static final Logger referenceLogger = LogManager.getLogger("reference_log");
    private static final Logger logger = LogManager.getLogger(ReplicaSender.class.getSimpleName());
    

    public ReplicaSender() {
        super();
    }

    public boolean sendMessage(String alias, String url, Timestamp expireDate) throws IOException{
        if (!isIOStreamsValid()) {
            logger.error("Outputstream is null");
            return false;
        }

        JSONObject msg = new JSONObject();
        msg.put("type", MessageType.INSERT_REQUEST);
        //msg.put("from", value)
        msg.put("alias", alias);
        msg.put("url", url);
        msg.put("expires", expireDate.toString());

        JSONObject response = sendJson(msg);

        if (response.getString("type").equals(MessageType.INSERT_CONFIRMATION)){
            referenceLogger.info(String.format(" REMOTE_WRITE_CONFIRMED(%s,%s) ", super.serverToConnect, alias));
            return true;
        }
        else{
            return false;
        }
    }

    // Test
    // public static void main(String[] args) {
    //     ReplicaSender sender = new ReplicaSender();
    //     int port_to_connect = Integer.parseInt(args[0]);
    //     sender.startConnection("localhost", port_to_connect);
    //     String response = "";
    //     String response2 = "";
    //     try {
    //         response = sender.sendMessage("hello from sender");
    //         response2 = sender.sendMessage("stop");
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    //     System.out.println("response: " + response + ", "+ response2);
    // }
}
