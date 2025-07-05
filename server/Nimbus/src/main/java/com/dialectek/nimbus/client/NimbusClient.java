package com.dialectek.nimbus.client;

import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

@ClientEndpoint
public class NimbusClient
{
   public static void main(String[] args)
   {
      String host = "localhost";

      if (args.length >= 1)
      {
         WSClient.id = args[0];
      }
      if (args.length == 2)
      {
         host = args[1];
      }
      ClientManager client = ClientManager.createClient();
      try
      {
         client.connectToServer(WSClient.class, new URI("ws://" + host + ":8025/ws/server"));
      }
      catch (DeploymentException | URISyntaxException e)
      {
         System.err.println("Cannot connect to server: " + e.getMessage());
         System.exit(1);
      }
      Scanner scanner = new Scanner(System.in);
      while (true)
      {
         try
         {
            Thread.sleep(3000);
         }
         catch (InterruptedException e1) {}
         System.out.print("Enter message or exit: ");
         String message = scanner.nextLine();
         if (message.equals("exit")) { break; }
         try
         {
            WSClient.session.getBasicRemote().sendText(message);
         }
         catch (IOException e)
         {
            System.err.println("Cannot send messaage: " + e.getMessage());
         }
      }
      scanner.close();
      System.exit(0);
   }
}
