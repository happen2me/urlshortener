package co.yuanchun.app;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) throws IOException
    {
        System.out.println( "Hello World!" );
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/test", new MyHttpHandler());
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);
        server.setExecutor(threadPoolExecutor);
        server.start();
    }

    private static class MyHttpHandler implements HttpHandler{

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requstParams = null;
            if("GET".equals(exchange.getRequestMethod())){
                requstParams = handleGetRequest(exchange);
            }
            else if("POST".equals(exchange.getRequestMethod())){
                requstParams = handlePostRequest(exchange);
            }
        }

        private String handleGetRequest(HttpExchange exchange){
            return exchange.getRequestURI()
                .toString()
                .split("\\?")[1]
                .split("=")[1];
        }

        private String handlePostRequest(HttpExchange exchange){
            return exchange.getRequestBody().toString();
        }
        
        private void handleResponse(HttpExchange exchange, String requestParams){
            OutputStream outputStream = exchange.getResponseBody();

        }


    }
}
