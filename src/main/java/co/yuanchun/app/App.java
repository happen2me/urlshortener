package co.yuanchun.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;

import co.yuanchun.app.logging.Log4JConfiguration;

public class App {
    private static final Logger logger = LogManager.getLogger(App.class.getName());
    private static final int ALIAS_LENGTH = 6;

    private int port;
    private String ip;
    private String databaseFilePath;
    private List<String> serverAddresses;

    public static void main(String[] args) {
        App server = new App(args);
        server.run();
    }

    private void run() {
        UrlShortenerServer server = new UrlShortenerServer(databaseFilePath, ip, port, 100);
        server.start();
      }

    private App(String[] args){
        setupLogging();
        parseCommandline(args);
    }

    private void setupLogging(){
        ConfigurationFactory.setConfigurationFactory(new Log4JConfiguration());
    }

    private void parseCommandline(String[] args) {
        CommandLineParser parser = new DefaultParser();
    
        Options options = new Options();
        options.addRequiredOption( "p", "port", true, "the port to use for the client server.");
        options.addRequiredOption( "i", "ip", true, "the ip address to use for the client server");
        options.addRequiredOption( "d", "database", true, "the file path to the sqlite database file");
        options.addOption("s", "servers", true, "the addresses of the other servers to connect to");
    
        HelpFormatter formatter = new HelpFormatter();
        try {
          CommandLine line = parser.parse( options, args );
    
          port = Integer.parseInt(line.getOptionValue("port"));
          ip = line.getOptionValue("ip");
          databaseFilePath = line.getOptionValue("database");
    
          if (line.hasOption("servers")) {
            String serverList = line.getOptionValue("servers");
            serverAddresses = Arrays.asList(serverList.split(","));
          } else {
            serverAddresses = new ArrayList<>();
          }
        }
        catch( ParseException exp ) {
          logger.error( "Error parsing the commandline:" + exp.getMessage());
          formatter.printHelp( "urlShortener", options );
        }
      }
}
