package com.dialectek.nimbus.server;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

@ServerEndpoint(value = "/server")
public class WSServer
{
   private static final String ID_TO_NAME_FILE_NAME         = "id_to_name.py";
   private static final String ID_TO_NAME_RESULTS_FILE_NAME = "id_to_name.txt";

   private Logger logger = Logger.getLogger(this.getClass().getName());

   @OnOpen
   public void onOpen(Session session)
   {
      logger.info("Connected ... " + session.getId());
   }


   @OnMessage
   public void onMessage(Session session, String msg)
   {
      logger.info("Message from client: " + msg);
      try
      {
         if ((msg != null) && msg.contains(":"))
         {
            String[] parts = msg.split(":");
            String op   = parts[0];
            String args = parts[1];
            if (op.equals("id_to_name"))
            {
               String id   = args;
               String name = idToName(id);
               if (name != null)
               {
                  NimbusServer.connections.put(name, session);
                  session.getBasicRemote().sendText("id_to_name:" + name);
               }
               else
               {
                  session.getBasicRemote().sendText("id_to_name:error");
               }
            }
            else if (op.equals("peer_message"))
            {
               parts = args.split(";");
               if ((parts != null) && (parts.length == 2))
               {
                  String  peer_name    = parts[0];
                  String  peer_msg     = parts[1];
                  Session peer_session = NimbusServer.connections.get(peer_name);
                  if (peer_session != null)
                  {
                     try
                     {
                        peer_session.getBasicRemote().sendText("peer_message:" + peer_msg);
                     }
                     catch (IOException e)
                     {
                        session.getBasicRemote().sendText("peer_message:cannot send message to " + peer_name);
                     }
                  }
                  else
                  {
                     session.getBasicRemote().sendText("peer_message:unknown peer name " + peer_name);
                  }
               }
               else
               {
                  session.getBasicRemote().sendText("peer_message:invalid message " + msg);
               }
            }
            else
            {
               session.getBasicRemote().sendText("unknown_message:" + msg);
            }
         }
         else
         {
            session.getBasicRemote().sendText("unknown_message:" + msg);
         }
      }
      catch (IOException e)
      {
         logger.info(e.getMessage());
      }
   }


   @OnClose
   public void onClose(Session session, CloseReason closeReason)
   {
      logger.info(String.format("Session %s closed because of %s", session.getId(), closeReason));
   }


   @OnError
   public void error(Session session, Throwable error)
   {
      logger.info("ERROR");
      logger.info("SessionId " + session.getId());
      logger.info("ErrorMsg " + error.getMessage());
   }


   // Get name from id.
   public String idToName(String id)
   {
      new File(ID_TO_NAME_RESULTS_FILE_NAME).delete();
      ProcessBuilder processBuilder = new ProcessBuilder("python", ID_TO_NAME_FILE_NAME, "id_to_name:" + id);
      processBuilder.inheritIO();
      Process process = null;
      try
      {
         process = processBuilder.start();
         if (process.waitFor() != 0)
         {
            logger.info("Error running " + ID_TO_NAME_FILE_NAME);
            return(null);
         }
      }
      catch (InterruptedException e) {}
      catch (IOException e)
      {
         logger.info("Cannot run " + ID_TO_NAME_FILE_NAME + ":" + e.getMessage());
         return(null);
      }
      String name = null;
      try
      {
         BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ID_TO_NAME_RESULTS_FILE_NAME)));
         name = br.readLine();
         br.close();
         if (name == null)
         {
            logger.info("Cannot read name file " + ID_TO_NAME_RESULTS_FILE_NAME);
         }
         return(name);
      }
      catch (IOException e)
      {
         logger.info("Cannot read name file " + ID_TO_NAME_RESULTS_FILE_NAME + ":" + e.getMessage());
         return(null);
      }
      catch (Exception e)
      {
         logger.info("Error processing name file " + ID_TO_NAME_RESULTS_FILE_NAME + ":" + e.getMessage());
         return(null);
      }
   }
}
