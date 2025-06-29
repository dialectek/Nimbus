package com.dialectek.nimbus.client;

import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;

@ClientEndpoint
public class NimbusClient {

    private static CountDownLatch latch;

    public static void main(String[] args) {
    	if (args.length == 1)
    	{
    		WSClient.id = args[0];
    	}
        latch = new CountDownLatch(1);
        ClientManager client = ClientManager.createClient();
        try {
            client.connectToServer(WSClient.class, new URI("ws://localhost:8025/ws/server"));
            latch.await();

        } catch (DeploymentException | URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
