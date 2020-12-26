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

import co.yuanchun.app.clientConnectionHandling.ClientGateway;
import co.yuanchun.app.replication.ServerIdentifier;

public class App {
    private static final Logger logger = LogManager.getLogger(App.class.getName());
    private static final int ALIAS_LENGTH = 6;

    private int port;
    private String ip;
    private String databaseFilePath;
    private List<ServerIdentifier> serverAddresses;
    private int replicationPort;

    public static void main(String[] args) {
        App server = new App(args);
        server.run();
    }

    private void run() {
        ClientGateway server = new ClientGateway(databaseFilePath, ip, port, 100, serverAddresses, replicationPort);
        server.start();
      }

    private App(String[] args){
        parseCommandline(args);
    }

    private void parseCommandline(String[] args) {
        CommandLineParser parser = new DefaultParser();
    
        Options options = new Options();
        options.addRequiredOption( "p", "port", true, "the port to use for the client server.");
        options.addRequiredOption( "i", "ip", true, "the ip address to use for the client server");
        options.addRequiredOption( "d", "database", true, "the file path to the sqlite database file");
        options.addRequiredOption( "l", "replicationListenPort", true, "the port to listen to incoming replications");
        options.addOption("s", "servers", true, "the addresses of the other servers to connect to");
    
        HelpFormatter formatter = new HelpFormatter();
        try {
          CommandLine line = parser.parse( options, args );
    
          port = Integer.parseInt(line.getOptionValue("port"));
          ip = line.getOptionValue("ip");
          databaseFilePath = line.getOptionValue("database");
          replicationPort = Integer.parseInt(line.getOptionValue("replicationListenPort"));
    
          if (line.hasOption("servers")) {
            String serverList = line.getOptionValue("servers");
            serverAddresses = new ArrayList<>();
            List<String> serverStrings = Arrays.asList(serverList.split(","));
            for (String serverString : serverStrings) {
              String[] values = serverString.split(":");
              String ip = values[0];
              int port = Integer.parseInt(values[1]);
              serverAddresses.add(new ServerIdentifier(ip, port));
            }
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
