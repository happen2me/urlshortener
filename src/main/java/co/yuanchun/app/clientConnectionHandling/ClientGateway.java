package co.yuanchun.app.clientConnectionHandling;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.yuanchun.app.Node;
import co.yuanchun.app.communication.ServerIdentifier;

/**
 * Hello world!
 *
 */
public class ClientGateway {
    private static final Logger referenceLogger = LogManager.getLogger("reference_log");
    private static final Logger logger = LogManager.getLogger(ClientGateway.class.getSimpleName());
    private HttpServer server;
    private ExecutorService threadPoolExecutor;

    private int port;
    private String ip;

    public ClientGateway(String databasePath, String ip, int port, int backlog, 
        List<ServerIdentifier> serverList, int replicationListenningPort) {
        this.port = port;
        this.ip = ip;
        try {
            server = HttpServer.create(new InetSocketAddress(port), backlog);
        } catch (IOException e) {
            throw new RuntimeException("Could not start http server on port " + port + " for IP address " + ip);
        }
        threadPoolExecutor = Executors.newFixedThreadPool(20);
        server.setExecutor(threadPoolExecutor);

        server.createContext("/", new ClientHttpHandler(databasePath, serverList,replicationListenningPort));
    }

    public void start() {
        logger.info("Starting server at " + ip + ":" + port);
        server.start();
    }

    public void stop() {
        server.stop(1);
        threadPoolExecutor.shutdown();
    }

    private static class ClientHttpHandler implements HttpHandler {
        private Node node;
        private static AtomicLong requestID = new AtomicLong(0);

        public ClientHttpHandler(String databasePath, List<ServerIdentifier> serverList, int replicationListenningPort) {
            node = new Node(databasePath, serverList, replicationListenningPort);
            node.initializeServices();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestParams = null;
            long id = requestID.incrementAndGet();
            if("GET".equals(exchange.getRequestMethod())){
                requestParams = handleGET(exchange, id);
                logger.debug("GET result: " + requestParams.toString());
            }
            else if("POST".equals(exchange.getRequestMethod())){
                requestParams = handlePost(exchange, id);                
            }
            handleResponse(exchange, requestParams, id);
        }

        private String handleGET(HttpExchange httpExchange, long id) throws IOException {        
            
            String alias = httpExchange.getRequestURI().getPath();   
            if (alias == null || alias == "/" || alias == "/ ") {
                logger.error("GET requests should have an alias.");
                httpExchange.sendResponseHeaders(400, 0);
                httpExchange.close();
                return "";
            }         
            alias = alias.substring(1);  // Removes the leading slash
            
            referenceLogger.info(String.format("RECEIVED_CLIENT_REQUEST(%d, GET,%s)", id, alias));
            String url = node.findAlias(alias);        
            if (url == "") {
                logger.info("Queried URL not found");
            }
            else{
                logger.info("Found url " + url);
            }            
            return url;
          }

        private String handlePost(HttpExchange exchange, long id){
            String requestString = null;
            try (InputStream requestStream = exchange.getRequestBody();) {
                requestString = tranlateInputStream(requestStream);
            } catch (Exception e) {
                logger.error("Can't parse POST request body", e);
                return "";
            }
            referenceLogger.info(String.format("RECEIVED_CLIENT_REQUEST(%d,POST,%s)", id, requestString));
            String alias = node.addUrl(requestString);
            logger.debug("Alias is " + alias);
            return alias;
        }

        private String tranlateInputStream(InputStream in){
            final StringBuilder sBuilder = new StringBuilder();
            final char[] buffer = new char[1024];
            try (Reader r = new InputStreamReader(in, "UTF-8")) {
                int len;
                while ((len = r.read(buffer)) != -1) {
                 sBuilder.append(buffer, 0, len);   
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // removed stripTrailing method
            String output = sBuilder.toString();
            return output;
        }
        
        private void handleResponse(HttpExchange exchange, String response, long id) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                if (response == "") {
                    exchange.sendResponseHeaders(404, 0);
                }
                else{
                    exchange.sendResponseHeaders(200, response.getBytes().length);                                    
                }
                OutputStream outputStream = exchange.getResponseBody();   
                outputStream.write(response.getBytes());
                outputStream.flush();
                outputStream.close();
                referenceLogger.info(String.format("SEND_CLIENT_REPONSE(%d,GET,%s)", id, 
                                                    response==""?"Not existing":response));
            }
            else if("POST".equals(exchange.getRequestMethod())){
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream out = exchange.getResponseBody();
                out.write(response.getBytes());
                out.close();
                referenceLogger.info(String.format("SEND_CLIENT_REPONSE(%d,POST,%s)", id, response));
            }
        }
    }
}