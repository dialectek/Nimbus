// Nimbus server.

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
            if (op.equals("request_name"))
            {
               String id   = args;
               String name = idToName(id);
               if (name != null)
               {
                  NimbusServer.connections.put(name, session);
                  session.getBasicRemote().sendText("request_name:name;" + name);
                  if (NimbusServer.agents.get(name) == null)
                  {
                     PrisonersDilemmaAgent agent = new PrisonersDilemmaAgent(name);
                     NimbusServer.agents.put(name, agent);
                  }
               }
               else
               {
                  session.getBasicRemote().sendText("request_name:error");
               }
            }
            else if (op.equals("peer_message"))
            {
               String from_name = NimbusServer.connections.getNameBySession(session);
               if (from_name != null)
               {
                  parts = args.split(";");
                  if ((parts != null) && (parts.length == 2))
                  {
                     String  to_name    = parts[0];
                     String  message    = parts[1];
                     Session to_session = NimbusServer.connections.getSessionByName(to_name);
                     if (to_session != null)
                     {
                        try
                        {
                           to_session.getBasicRemote().sendText("peer_message:contents;" + from_name + ";" + message);
                        }
                        catch (IOException e)
                        {
                           session.getBasicRemote().sendText("peer_message:error;cannot send message to " + to_name);
                        }
                     }
                     else
                     {
                        session.getBasicRemote().sendText("peer_message:error;unknown name " + to_name);
                     }
                  }
                  else
                  {
                     session.getBasicRemote().sendText("peer_message:error;invalid message " + msg);
                  }
               }
               else
               {
                  session.getBasicRemote().sendText("peer_message:error;unknown origin name");
               }
            }
            else if (op.equals("game_invite"))
            {
               String agentA = NimbusServer.connections.getNameBySession(session);
               if (agentA != null)
               {
                  parts = args.split(";");
                  if ((parts != null) && (parts.length == 1))
                  {
                     String agentB = parts[0];
                     if (!agentA.equals(agentB))
                     {
                        Session agentB_session = NimbusServer.connections.getSessionByName(agentB);
                        if (agentB_session != null)
                        {
                           if (NimbusServer.games.get(agentA) != null)
                           {
                              session.getBasicRemote().sendText("game_invite:error;game in progress");
                           }
                           else if (NimbusServer.games.get(agentB) != null)
                           {
                              session.getBasicRemote().sendText("game_invite:error;agent " + agentB + " in another game");
                           }
                           else
                           {
                              try
                              {
                                 agentB_session.getBasicRemote().sendText("game_invite:" + agentA);
                                 PrisonersDilemmaGame game = new PrisonersDilemmaGame(agentA, agentB);
                                 NimbusServer.games.put(agentA, game);
                                 NimbusServer.games.put(agentB, game);
                                 session.getBasicRemote().sendText("game_invite:sent;invite sent to " + agentB);
                              }
                              catch (IOException e)
                              {
                                 session.getBasicRemote().sendText("game_invite:error;cannot invite " + agentB);
                              }
                           }
                        }
                        else
                        {
                           session.getBasicRemote().sendText("game_invite:error;unknown agent " + agentB);
                        }
                     }
                     else
                     {
                        session.getBasicRemote().sendText("game_invite:error;invalid agent " + agentB);
                     }
                  }
                  else
                  {
                     session.getBasicRemote().sendText("game_invite:error;invalid message " + msg);
                  }
               }
               else
               {
                  session.getBasicRemote().sendText("game_invite:error;unknown origin name");
               }
            }
            else if (op.equals("game_accept"))
            {
               String agentB = NimbusServer.connections.getNameBySession(session);
               if (agentB != null)
               {
                  parts = args.split(";");
                  if ((parts != null) && (parts.length == 1))
                  {
                     PrisonersDilemmaGame game = NimbusServer.games.get(agentB);
                     if (game != null)
                     {
                        if ((game.state == PrisonersDilemmaGame.INVITE) && game.agentB.equals(agentB))
                        {
                           String  agentA         = game.agentA;
                           Session agentA_session = NimbusServer.connections.getSessionByName(agentA);
                           if (agentA_session != null)
                           {
                              try
                              {
                                 agentA_session.getBasicRemote().sendText("game_accept:" + agentB);
                                 game.state = PrisonersDilemmaGame.ACTIVE;
                                 session.getBasicRemote().sendText("game_accept:sent;accept sent to " + agentA);
                              }
                              catch (IOException e)
                              {
                                 session.getBasicRemote().sendText("game_invite:error;cannot invite " + agentB);
                              }
                           }
                           else
                           {
                              session.getBasicRemote().sendText("game_invite:error;unknown agent " + agentA);
                           }
                        }
                        else
                        {
                           session.getBasicRemote().sendText("game_accept:error;invalid game");
                        }
                     }
                     else
                     {
                        session.getBasicRemote().sendText("game_accept:error;game not found");
                     }
                  }
                  else
                  {
                     session.getBasicRemote().sendText("game_invite:error;invalid message " + msg);
                  }
               }
               else
               {
                  session.getBasicRemote().sendText("game_accept:error;unknown origin name");
               }
            }
            else if (op.equals("game_quit"))
            {
               String agent = NimbusServer.connections.getNameBySession(session);
               if (agent != null)
               {
                  PrisonersDilemmaGame game = NimbusServer.games.get(agent);
                  if (game != null)
                  {
                     if (game.agentA.equals(agent) || game.agentB.equals(agent))
                     {
                        String  agentA         = game.agentA;
                        Session agentA_session = NimbusServer.connections.getSessionByName(agentA);
                        if (agentA_session != null)
                        {
                           try
                           {
                              agentA_session.getBasicRemote().sendText("game_quit:notify");
                           }
                           catch (IOException e)
                           {
                              session.getBasicRemote().sendText("game_quit:error;cannot notify " + agentA);
                           }
                        }
                        else
                        {
                            session.getBasicRemote().sendText("game_quit:error;unknown agent " + agentA);                        	
                        }
                        NimbusServer.games.remove(agentA);                        
                        String  agentB         = game.agentB;
                        Session agentB_session = NimbusServer.connections.getSessionByName(agentA);
                        if (agentB_session != null)
                        {
                           try
                           {
                              agentB_session.getBasicRemote().sendText("game_quit:notify");
                           }
                           catch (IOException e)
                           {
                              session.getBasicRemote().sendText("game_quit:error;cannot notify " + agentB);
                           }
                        }
                        else
                        {
                            session.getBasicRemote().sendText("game_quit:error;unknown agent " + agentB);                        	
                        }
                        NimbusServer.games.remove(agentB);
                     }
                     else
                     {
                        session.getBasicRemote().sendText("game_quit:error;invalid agent");
                     }
                  }
                  else
                  {
                     session.getBasicRemote().sendText("game_quit:error;game not found");
                  }
               }
               else
               {
                  session.getBasicRemote().sendText("game_quit:error;unknnown origin name");
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
