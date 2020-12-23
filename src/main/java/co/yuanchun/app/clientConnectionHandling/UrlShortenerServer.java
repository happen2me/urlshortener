package co.yuanchun.app.clientConnectionHandling;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.yuanchun.app.UrlShortener;

/**
 * Hello world!
 *
 */
public class UrlShortenerServer {
    private static final Logger logger = LogManager.getLogger(UrlShortenerServer.class.getName());
    private HttpServer server;
    private ExecutorService threadPoolExecutor;
    

    private int port;
    private String ip;

    public UrlShortenerServer(String databasePath, String ip, int port, int backlog){
        this.port = port;
        this.ip = ip;
        try {
            server = HttpServer.create(new InetSocketAddress(ip, port), backlog);
        } catch (IOException e) {
            throw new RuntimeException("Could not start http server on port " + port + " for IP address " + ip);
        }
        threadPoolExecutor = Executors.newFixedThreadPool(20);
        server.setExecutor(threadPoolExecutor);

        server.createContext("/", new MyHttpHandler(databasePath));
    }

    public void start(){
        logger.info("Starting server at " + ip + ":" + port);
        server.start();
    }

    public void stop(){
        server.stop(1);
        threadPoolExecutor.shutdown();
    }

    private static class MyHttpHandler implements HttpHandler {
        private UrlShortener shortener;

        public MyHttpHandler(String databasePath) {
            shortener = new UrlShortener(databasePath);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("handling");
            String requestParams = null;
            if("GET".equals(exchange.getRequestMethod())){
                requestParams = handleGET(exchange);
                logger.debug(requestParams.toString());
            }
            else if("POST".equals(exchange.getRequestMethod())){
                requestParams = handlePostRequest(exchange);
            }
            handleResponse(exchange, requestParams);
        }

        private String handleGET(HttpExchange httpExchange) throws IOException {        
            String alias = httpExchange.getRequestURI().getPath();
            if (alias == null) {
              logger.error("GET requests should have an alias.");
              httpExchange.sendResponseHeaders(400, 0);
              httpExchange.close();
              return "";
            }
            alias = alias.substring(1);  // Removes the leading slash
            logger.info(String.format("RECEIVED_CLIENT_REQUEST(GET,%s)", alias));
                String url = shortener.findAlias(alias);        
                if (url == "") {
                    logger.info("Queried URL not found");
                }
                return url;
          }

        private String handlePostRequest(HttpExchange exchange){
            String requestString = null;
            try (InputStream requestStream = exchange.getRequestBody();) {
                requestString = tranlateInputStream(requestStream);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
            logger.debug("Request body is:" + requestString);
            String alias = shortener.insertUrl(requestString);
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
            String output = sBuilder.toString().stripTrailing();
            return output;
        }
        
        private void handleResponse(HttpExchange exchange, String response) throws IOException {
            OutputStream outputStream = exchange.getResponseBody();
            exchange.sendResponseHeaders(200, response.length());
            outputStream.write(response.getBytes());
            outputStream.flush();
            outputStream.close();
        }

    }
}