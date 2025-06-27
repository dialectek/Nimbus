package com.dialectek.nimbus.server;

import org.glassfish.tyrus.server.Server;

public class NimbusServer {

    public static void main(String[] args) {
        runServer();
    }

    public static void runServer() {
        Server server = new Server("localhost", 8025, "/ws", WSServer.class);
        try {
            server.start();
            while (true) {
            	Thread.sleep(1000);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            server.stop();
        }
    }
}