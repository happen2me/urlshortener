package co.yuanchun.app.communication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.yuanchun.app.DatabaseAdaper;
import co.yuanchun.app.cache.CacheService;

public class MessageService {
    private static final Logger logger = LogManager.getLogger(MessageService.class.getSimpleName());

    private Thread receiverThread;
    private DatabaseAdaper database;
    private CacheService cache;

    public MessageService(DatabaseAdaper database, CacheService cache){
        this.database = database;
        this.cache = cache;
    }

    public void startListen(int port) {
        logger.info("Start listening incoming replication on port " + port);
        receiverThread = new Thread(new MessageReceiver(port, database, cache));
        receiverThread.start();
    }

    public void stopListen() {
        // TODO: implement asyncronous stop method
    }
    
}
