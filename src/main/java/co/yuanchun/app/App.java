package co.yuanchun.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.plaf.synth.SynthStyleFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws IOException {

        System.out.println("Initializing databse");

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new MyHttpHandler());
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        server.setExecutor(threadPoolExecutor);
        server.start();
    }

    private static class MyHttpHandler implements HttpHandler {
        UrlShortener shortener;

        public MyHttpHandler() {
            shortener = new UrlShortener();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestParams = null;
            if("GET".equals(exchange.getRequestMethod())){
                requestParams = handleGetRequest(exchange);
            }
            else if("POST".equals(exchange.getRequestMethod())){
                requestParams = handlePostRequest(exchange);
            }
            handleResponse(exchange, requestParams);
        }

        private String handleGetRequest(HttpExchange exchange){
            String query = exchange.getRequestURI().getQuery().toString();
            System.out.println("Get query: " + query);
            String urlFound = shortener.findAlias(query);
            if(urlFound == ""){
                System.out.println("Queried alias not found.");
            }
            return urlFound;
        }

        private String handlePostRequest(HttpExchange exchange){
            String requestString = null;
            try (InputStream requestStream = exchange.getRequestBody();) {
                requestString = tranlateInputStream(requestStream);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
            System.out.print("Request body is:");
            System.out.println(requestString);
            String alias = shortener.insertUrl(requestString, null);
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
