package co.yuanchun.app.cache;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import co.yuanchun.app.communication.MessageSender;
import co.yuanchun.app.communication.MessageType;
import co.yuanchun.app.communication.ServerIdentifier;

public class ForwardSender extends MessageSender {
    private static final Logger logger = LogManager.getLogger(ForwardSender.class.getSimpleName());
    
    public ForwardSender() {
        super();
    }

    public JSONObject forwardQuery(ServerIdentifier dest, String alias) {
        JSONObject msg = new JSONObject();
        msg.put("type", MessageType.READ_FORWARD_REQUEST);
        msg.put("alias", alias);
        JSONObject response;
        try {
            response = sendJson(msg);
        } catch (IOException e) {
            logger.error("Error forwarding query", e);
            return null;
        }
        return response;
    }
}
