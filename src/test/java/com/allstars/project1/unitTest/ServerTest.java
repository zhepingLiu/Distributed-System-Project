package com.allstars.project1.unitTest;

import com.allstars.project1.Server;
import com.allstars.project1.Static;
import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Zheping on 2017/4/25.
 */
class ServerTest {

    private static final String HOST = "localhost";
    private static final int PORT = 2333;
    private static final int TIMEOUT = 10000;

    class DummyClient {

        Socket socket;
        DataOutputStream out;
        DataInputStream in;

        void connectToServer() {

            try {

                socket = new Socket(HOST, PORT);
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                socket.setSoTimeout(TIMEOUT);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    @BeforeEach
    void setUp() {
    }

    @Disabled
    @Test
    void getOptions() {
    }

    @Disabled
    @Test
    void startServer() {
        int connectionIntervalLimit = Static.DEFAULT_CONNECTION_INTERVAL;
        int exchangeIntervalLimit = Static.DEFAULT_EXCHANGE_INTERVAL;
        String secret = "JackBigLeg";

        try {
            Server.startServer(connectionIntervalLimit, exchangeIntervalLimit, secret, HOST, PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Disabled
    @Test
    void main() {

        ArrayList<String[]> args = new ArrayList<String[]>();

        String[] arg = {"java", "Server.java", "-advertisedhostname", "localhost", "-port", "2333", "-secret", "JackBigLeg", "-debug"};

        Server.main(arg);

        Server.stop();
    }
}