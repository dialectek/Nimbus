// Nimbus server.

package com.dialectek.nimbus.server;

import java.util.concurrent.CountDownLatch;
import org.glassfish.tyrus.server.Server;

public class NimbusServer
{
   // Connections.
   public static NameToSessionMap connections;

   public static void main(String[] args)
   {
      connections = new NameToSessionMap();
      runServer();
   }


   private static CountDownLatch latch;

   public static void runServer()
   {
      latch = new CountDownLatch(1);
      Server server = new Server("localhost", 8025, "/ws", WSServer.class );
      try {
         server.start();
         latch.await();
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
      finally {
         server.stop();
      }
   }
}
