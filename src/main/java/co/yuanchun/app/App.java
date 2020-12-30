package co.yuanchun.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import co.yuanchun.app.communication.ServerIdentifier;

public class App {
    private static final Logger logger = LogManager.getLogger(App.class.getSimpleName());
    private static final int ALIAS_LENGTH = 6;

    private int port;
    private String ip;
    private String databaseFilePath;
    private List<ServerIdentifier> serverAddresses;
    private int replicationPort;
    private static ThreadLocal<HashMap<String, String>> globalVars = new ThreadLocal<>();

    public static void main(String[] args) {
        App server = new App(args);
        server.run();
    }

    private void run() {
        ClientGateway server = new ClientGateway(databaseFilePath, ip, port, 100, serverAddresses, replicationPort);
        server.start();
    }

    private App(String[] args) {
        globalVars.set(new HashMap<String, String>());
        parseCommandline(args);
    }

    private void parseCommandline(String[] args) {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addRequiredOption("p", "port", true, "the http port to use for the client server.");
        options.addRequiredOption("i", "ip", true, "the ip address to use for the client server");
        options.addRequiredOption("d", "database", true, "the file path to the sqlite database file");
        options.addRequiredOption("l", "msg_port", true, "the port to listen to incoming replications");
        options.addOption("s", "servers", true, "the addresses of the other servers to connect to");
        options.addOption("init", "initial_dataset", true, "the initial dataset location to bulk load");

        HelpFormatter formatter = new HelpFormatter();
        try {
            CommandLine line = parser.parse(options, args);

            port = Integer.parseInt(line.getOptionValue("port"));
            ip = line.getOptionValue("ip");
            databaseFilePath = line.getOptionValue("database");
            replicationPort = Integer.parseInt(line.getOptionValue("msg_port"));

            SetGlobalVar("local_ip", ip);
            SetGlobalVar("http_port", String.valueOf(port));
            SetGlobalVar("msg_port", String.valueOf(replicationPort));

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
                logger.debug(serverAddresses.size() + " other nodes are specified as:");
                for (ServerIdentifier serverIdentifier : serverAddresses) {
                    logger.debug("ip: " + serverIdentifier.getIp() + "; port: " + serverIdentifier.getPort());
                }
            } else {
                serverAddresses = new ArrayList<>();
            }
            if (line.hasOption("initial_dataset")) {
                // put initial dataset path to global vars
                SetGlobalVar("initial_dataset", line.getOptionValue("initial_dataset"));
            }

        } catch (ParseException exp) {
            logger.error("Error parsing the commandline:" + exp.getMessage());
            formatter.printHelp("urlShortener", options);
        }
    }

    public static HashMap<String, String> GetGloablVarsMap() {
        return globalVars.get();
    }

    public static void SetGlobalVar(String key, String value) {
        HashMap<String, String> map = globalVars.get();
        map.put(key, value);
    }

    public static String GetGlobalVar(String key) {
        HashMap<String, String> map = globalVars.get();
        return map.get(key);
    }

    public static ServerIdentifier GetIdentifier(){
        String ip = GetGlobalVar("local_ip");
        int msgPort = Integer.valueOf(GetGlobalVar("msg_port"));
        return new ServerIdentifier(ip, msgPort);
    }
}
