package EzShare;

import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;

import org.apache.commons.cli.*;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * EzShare server implementation, has a main method to be used through command line
 */
public class Server {
    public static ResourceStorage resourceStorage = new ResourceStorage();
    public static Set<EzServer> secureserverList = Collections.synchronizedSet(new HashSet<>());
    public static Set<EzServer> insecureserverList = Collections.synchronizedSet(new HashSet<>());
    public static HashMap<SocketAddress, Date> lastConnectionTime = new HashMap<>();
    public static EzServer self;

    private static boolean running = false;
    private static Thread mainThread;

    public static boolean isRunning() {
        return running;
    }

    /**
     * Terminate server activity (for testing)
     */
    public static void stop() {
        running = false;
        mainThread.interrupt();
    }

    /**
     * Block current thread until server is ready for connection (for testing)
     *
     * @throws InterruptedException Interrupted during sleep
     */
    public static void waitUntilReady() throws InterruptedException {
        while (!Server.isRunning()) { // busy waiting
            Thread.sleep(100);
        }
    }

    /**
     * Setup insecure server, open socket, start listening to connections
     *
     * @param connectionIntervalLimit the time interval between each new connection from clients
     * @param exchangeInterval        the time interval between each auto-exchange
     * @param secret                  the secret password from the Server
     * @param host                    the host address for the server
     * @param port                    the port number for the server
     * @throws IOException
     */
    public static void startServer(int connectionIntervalLimit, int exchangeInterval, String secret, String host
            , int port) throws IOException {
        self = new EzServer(host, port);
        ServerSocket listenSocket = null;
        try {
            // for sending exchange request to other servers
            ExchangeThread exchangeThread = new ExchangeThread(exchangeInterval, insecureserverList);
            exchangeThread.start();

            listenSocket = new ServerSocket(port);
            int i = 0;
            Logging.logInfo("Server initialisation complete");
            running = true;
            while (running) {
                //TODO relay and exchange
                // wait for new client
                Logging.logInfo("Server listening for a connection");
                Socket clientSocket = listenSocket.accept();
                SocketAddress clientAddress = clientSocket.getRemoteSocketAddress();
                i++;
                Logging.logInfo("Received connection " + i);
                // start a new thread handling the client
                // TODO limitation on total number of threads
                ServiceThread c = new ServiceThread(lastConnectionTime, clientSocket, secret, resourceStorage,secureserverList,insecureserverList, self);
                c.start();
                Thread.sleep(connectionIntervalLimit);
            }
        } catch (InterruptedException e) {
            if (running) {
                e.printStackTrace();
            } else {
                listenSocket.close();
                Logging.logInfo("Server shutting down...");
            }
        }
    }




    /**
     * Setup secure server, open socket, start listening to connections
     *
     * @param connectionIntervalLimit the time interval between each new connection from clients
     * @param exchangeInterval        the time interval between each auto-exchange
     * @param secret                  the secret password from the Server
     * @param host                    the host address for the server
     * @param sport                    the port number for the server
     * @throws IOException
     */
    public static void startSServer(int connectionIntervalLimit, int exchangeInterval, String secret, String host
            , int sport) throws IOException {
        self = new EzServer(host, sport);
        SSLServerSocketFactory sslserversocketfactory =
                (SSLServerSocketFactory)
                SSLServerSocketFactory.getDefault();
        SSLServerSocket sslserversocket =
                (SSLServerSocket)
                sslserversocketfactory.createServerSocket(sport);
        try {
            int i = 0;
            Logging.logInfo("Server initialisation complete");
            running = true;
            while (running) {
                // wait for new client
                Logging.logInfo("Server listening for a connection");
                SSLSocket sslsocket = (SSLSocket) sslserversocket.accept();
                i++;
                Logging.logInfo("Received connection " + i);
                // start a new thread handling the client
                // TODO limitation on total number of threads
                ServiceThread c = new ServiceThread(lastConnectionTime, sslsocket, secret, resourceStorage, secureserverList, insecureserverList, self);
                c.start();
                Thread.sleep(connectionIntervalLimit);
            }
        }catch (InterruptedException e) {
                if (running) {
                    e.printStackTrace();
                } else {
                    sslserversocket.close();
                    Logging.logInfo("Server shutting down...");
                }
        }
    }

    /**
     * Get command line options
     *
     * @param args command line arguments
     * @return
     * @throws ParseException
     */
    public static CommandLine getOptions(String[] args) throws ParseException {
        Options options = new Options();

        options.addOption(Option.builder("advertisedhostname").desc("advertised hostname")
                .hasArg().type(String.class).build());
        options.addOption(Option.builder("connectionintervallimit").desc("connection interval limit in seconds")
                .hasArg().type(Integer.class).build());
        options.addOption(Option.builder("exchangeinterval").desc("exchange interval in seconds")
                .hasArg().type(Integer.class).build());
        options.addOption(Option.builder("sport").desc("secure server port, an integer")
                .hasArg().type(Integer.class).build());
        options.addOption(Option.builder("port").desc("server port, an integer")
                .hasArg().type(Integer.class).build());
        options.addOption(Option.builder("secret").desc("secret")
                .hasArg().type(String.class).build());
        options.addOption(Option.builder("debug").desc("print debug information").build());

        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    /**
     * Main function of Server, used through command line
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        CommandLine cmd = null;
        Static.configSecurity();
        try {
            cmd = getOptions(args);
        } catch (ParseException e) {
            Logging.logInfo("Command line arguments missing or invalid, please try again");
            return;
        }

        mainThread = Thread.currentThread();

        // set debug
        Logging.setEnablePrint(cmd.hasOption("debug"));
        if (cmd.hasOption("debug")) {
            Logging.logInfo("setting debug on");
        }

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

        Logging.logInfo("Server secret: " + secret);

        try {

            // determine host
            InetAddress address = InetAddress.getLocalHost();
            String host = cmd.getOptionValue("advertisedhostname", address.getHostName());

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

            // set default sport value
            int sport = Static.DEFAULT_SPORT, port;
            try {
                // determine whether it is a port or sport where sport has higher precedence
                if (!cmd.hasOption("sport")&&!cmd.hasOption("port")) {
                    startSServer(connectionIntervalLimit, exchangeInterval, secret, host, sport);
                }
                if(cmd.hasOption("sport")) {
                    sport = Integer.parseInt(cmd.getOptionValue("sport"));
                    // Start secure server
                    startSServer(connectionIntervalLimit, exchangeInterval, secret, host, sport);
                }
                if(cmd.hasOption("port")) {
                    port = Integer.parseInt(cmd.getOptionValue("port"));
                    // Start insecure server
                    startServer(connectionIntervalLimit, exchangeInterval, secret, host, port);
                }
            }
            catch (BindException e) {
                Logging.logInfo("Port already taken, exiting...");
            }
            catch (Exception e) {
                Logging.logInfo("Unknown Exception in Server main thread, exiting...");
            }



        } catch (IOException e) {
            Logging.logInfo("Unknown IOException in Server main thread, exiting...");
        } catch (Exception e) {
            Logging.logInfo("Unknown Exception in Server main thread, exiting...");
        }
    }
}