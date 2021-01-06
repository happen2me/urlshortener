package co.yuanchun.app.cache;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import co.yuanchun.app.communication.MessageSender;
import co.yuanchun.app.communication.MessageType;
import co.yuanchun.app.communication.ServerIdentifier;

public class ForwardSender extends MessageSender {
    private static final Logger referenceLogger = LogManager.getLogger("reference_log");
    private static final Logger logger = LogManager.getLogger(ForwardSender.class.getSimpleName());

    private ServerIdentifier selfIdentifier;
    
    public ForwardSender(ServerIdentifier selfIdentifier) {
        super();
        this.selfIdentifier = selfIdentifier;
    }

    public JSONObject forwardQuery(ServerIdentifier dest, String alias) {
        JSONObject msg = new JSONObject();
        msg.put("type", MessageType.READ_FORWARD_REQUEST);
        msg.put("alias", alias);
        msg.put("from", selfIdentifier.toString());
        JSONObject response;
        try {
            logger.debug("forwarding alias " + alias + " to " + dest);
            response = sendJson(msg);
            referenceLogger.info(String.format("READ_FORWARDED(%s,%s)", dest, alias));
        } catch (IOException e) {
            logger.error("Error forwarding query", e);
            return null;
        }
        return response;
    }
}
