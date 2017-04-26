package com.allstars.project1;

import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;

import org.apache.commons.cli.*;

public class Server {
    public static ResourceStorage resourceStorage = new ResourceStorage();
    public static Set<EzServer> serverList = Collections.synchronizedSet(new HashSet<>());
    public static HashMap<SocketAddress, Date> lastConnectionTime = new HashMap<>();
    public static EzServer self;

    private static boolean running = false;
    private static Thread mainThread;

    public static boolean isRunning() {
        return running;
    }

    public static void stop() {
        running = false;
        mainThread.interrupt();
    }

    public static void waitUntilReady() throws InterruptedException {
        while (!Server.isRunning()) { // busy waiting
            Thread.sleep(100);
        }
    }

    public static void startServer(int connectionIntervalLimit, int exchangeInterval, String secret, String host, int port) {
        self = new EzServer(host, port);
        try {
            // for sending exchange request to other servers
            // TODO finish this and enable
            ExchangeThread exchangeThread = new ExchangeThread(exchangeInterval, serverList);
            exchangeThread.start();

            ServerSocket listenSocket = new ServerSocket(port);
            int i = 0;
            Logging.logInfo("Server initialisation complete");
            running = true;
            while (running) {
                // wait for new client
                Logging.logInfo("Server listening for a connection");
                Socket clientSocket = listenSocket.accept();
                SocketAddress clientAddress = clientSocket.getRemoteSocketAddress();
                i++;
                Logging.logInfo("Received connection " + i );
                // start a new thread handling the client
                // TODO limitation on total number of threads
                ServiceThread c = new ServiceThread(lastConnectionTime, clientSocket, secret, resourceStorage, serverList);
                c.start();
                Thread.sleep(connectionIntervalLimit);
            }
        } catch(IOException e) {
            Logging.logInfo("Listen socket:"+e.getMessage());
        } catch (InterruptedException e) {
            if (running) {
                e.printStackTrace();
            } else {
                Logging.logInfo("Server shutting down...");
            }
        }
    }

    public static CommandLine getOptions(String[] args) {
        Options options = new Options();

        options.addOption(Option.builder("advertisedhostname").desc("advertised hostname")
                .hasArg().type(String.class).build());
        options.addOption(Option.builder("connectionintervallimit").desc("connection interval limit in seconds")
                .hasArg().type(Integer.class).build());
        options.addOption(Option.builder("exchangeinterval").desc("exchange interval in seconds")
                .hasArg().type(Integer.class).build());
        options.addOption(Option.builder("port").desc("server port, an integer")
                .required().hasArg().type(Integer.class).build());
        options.addOption(Option.builder("secret").desc("secret")
                .hasArg().type(String.class).build());
        options.addOption(Option.builder("debug").desc("print debug information").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            Logging.logInfo("parse exception");
        }

        return cmd;
    }

    public static void main(String[] args) {
        CommandLine cmd = getOptions(args);

        mainThread = Thread.currentThread();

        // set debug
        Logging.setEnablePrint(cmd.hasOption("debug"));
        Logging.logInfo("setting debug on");

        // determine secret
        String secret = null;
        if (!cmd.hasOption("secret")) {
            // generate random secret
            // from http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
            SecureRandom random = new SecureRandom();
            secret = new BigInteger(130, random).toString(32);
        } else {
            secret = cmd.getOptionValue("secret");
        }


        try {
            // determine host and port
            InetAddress address = InetAddress.getLocalHost();
            String host = cmd.getOptionValue("advertisedhostname", address.getHostName());
            int port = Integer.parseInt(cmd.getOptionValue("port"));

            // set intervals
            int connectionIntervalLimit;
            if (cmd.hasOption("connectionintervallimit")) {
                connectionIntervalLimit = Integer.parseInt(cmd.getOptionValue("connectionintervallimit"));
            } else {
                connectionIntervalLimit = Static.DEFAULT_CONNECTION_INTERVAL;
            }
            int exchangeInterval;
            if (cmd.hasOption("exchangeinterval")) {
                exchangeInterval = Integer.parseInt(cmd.getOptionValue("exchangeinterval"));
            } else {
                exchangeInterval = Static.DEFAULT_EXCHANGE_INTERVAL;
            }

            // start the server
            startServer(connectionIntervalLimit, exchangeInterval, secret, host, port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}