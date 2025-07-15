// Nimbus server.

package com.dialectek.nimbus.server;

import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import org.glassfish.tyrus.server.Server;

public class NimbusServer
{
   // Connections.
   public static NameToSessionMap connections;

   // Prisoner's dilemma agents.
   public static TreeMap<String, PrisonersDilemmaAgent> agents;

   // Prisoner's dilemma games.
   public static TreeMap<String, PrisonersDilemmaGame> games;

   public static void main(String[] args)
   {
      connections = new NameToSessionMap();
      agents      = new TreeMap<String, PrisonersDilemmaAgent>();
      games       = new TreeMap<String, PrisonersDilemmaGame>();
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
