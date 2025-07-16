// Nimbus server.

package com.dialectek.nimbus.server;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
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
         String op   = "";
         String args = "";
         if ((msg != null))
         {
            if (msg.contains(":"))
            {
               String[] parts = msg.split(":");
               op             = parts[0];
               args           = parts[1];
            }
            else
            {
               op = msg;
            }
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
                  String[] parts = args.split(";");
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
                  String agentB = args;
                  if (!agentA.equals(agentB))
                  {
                     Session agentB_session = NimbusServer.connections.getSessionByName(agentB);
                     if (agentB_session != null)
                     {
                        if (NimbusServer.games.get(agentA) != null)
                        {
                           session.getBasicRemote().sendText("game_invite:error;game in progress for " + agentA);
                        }
                        else if (NimbusServer.games.get(agentB) != null)
                        {
                           session.getBasicRemote().sendText("game_invite:error;game in progress for " + agentB);
                        }
                        else
                        {
                           try
                           {
                              agentB_session.getBasicRemote().sendText("game_invite:request;" + agentA);
                              PrisonersDilemmaGame game = new PrisonersDilemmaGame(agentA, agentB);
                              NimbusServer.games.put(agentA, game);
                              NimbusServer.games.put(agentB, game);
                              session.getBasicRemote().sendText("game_invite:confirm;invite sent to " + agentB);
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
                  session.getBasicRemote().sendText("game_invite:error;unknown origin name");
               }
            }
            else if (op.equals("game_accept"))
            {
               String agentB = NimbusServer.connections.getNameBySession(session);
               if (agentB != null)
               {
                  String               agentA = args;
                  PrisonersDilemmaGame game   = NimbusServer.games.get(agentB);
                  if (game != null)
                  {
                     if ((game.state == PrisonersDilemmaGame.INVITE) &&
                         game.agentA.equals(agentA) && game.agentB.equals(agentB))
                     {
                        Session agentA_session = NimbusServer.connections.getSessionByName(agentA);
                        if (agentA_session != null)
                        {
                           try
                           {
                              agentA_session.getBasicRemote().sendText("game_accept:confirm;" + agentB);
                              game.state = PrisonersDilemmaGame.ACTIVE;
                              session.getBasicRemote().sendText("game_accept:confirm;accept sent to " + agentA);
                           }
                           catch (IOException e)
                           {
                              session.getBasicRemote().sendText("game_accept:error;cannot invite " + agentB);
                           }
                        }
                        else
                        {
                           session.getBasicRemote().sendText("game_accept:error;unknown agent " + agentA);
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
            else if (op.equals("game_action"))
            {
               String agent = NimbusServer.connections.getNameBySession(session);
               if (agent != null)
               {
                  String action = args;
                  if (action.equals("cooperate") || action.equals("betray"))
                  {
                     PrisonersDilemmaGame game = NimbusServer.games.get(agent);
                     if (game != null)
                     {
                        if (game.state == PrisonersDilemmaGame.ACTIVE)
                        {
                           if (game.agentA.equals(agent) || game.agentB.equals(agent))
                           {
                              if (game.agentA.equals(agent))
                              {
                                 if (game.agentAaction == PrisonersDilemmaGame.PENDING)
                                 {
                                    if (action.equals("cooperate"))
                                    {
                                       game.agentAaction = PrisonersDilemmaGame.COOPERATE;
                                    }
                                    else
                                    {
                                       game.agentAaction = PrisonersDilemmaGame.BETRAY;
                                    }
                                    if ((game.agentAaction != PrisonersDilemmaGame.PENDING) &&
                                        (game.agentBaction != PrisonersDilemmaGame.PENDING))
                                    {
                                       finishGame(game);
                                    }
                                    else
                                    {
                                       session.getBasicRemote().sendText("game_action:confirm;" + action);
                                    }
                                 }
                                 else
                                 {
                                    session.getBasicRemote().sendText("game_action:error;duplicate action");
                                 }
                              }
                              else
                              {
                                 if (game.agentBaction == PrisonersDilemmaGame.PENDING)
                                 {
                                    if (action.equals("cooperate"))
                                    {
                                       game.agentBaction = PrisonersDilemmaGame.COOPERATE;
                                    }
                                    else
                                    {
                                       game.agentBaction = PrisonersDilemmaGame.BETRAY;
                                    }
                                    if ((game.agentAaction != PrisonersDilemmaGame.PENDING) &&
                                        (game.agentBaction != PrisonersDilemmaGame.PENDING))
                                    {
                                       finishGame(game);
                                    }
                                    else
                                    {
                                       session.getBasicRemote().sendText("game_action:confirm;" + action);
                                    }
                                 }
                                 else
                                 {
                                    session.getBasicRemote().sendText("game_action:error;duplicate action");
                                 }
                              }
                           }
                           else
                           {
                              session.getBasicRemote().sendText("game_action:error;invalid agent " + agent);
                           }
                        }
                        else
                        {
                           session.getBasicRemote().sendText("game_action:error;game not active");
                        }
                     }
                     else
                     {
                        session.getBasicRemote().sendText("game_action:error;game not found");
                     }
                  }
                  else
                  {
                     session.getBasicRemote().sendText("game_action:error;invalid action");
                  }
               }
               else
               {
                  session.getBasicRemote().sendText("game_action:error;unknown origin name");
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
                        Session agentB_session = NimbusServer.connections.getSessionByName(agentB);
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
                  session.getBasicRemote().sendText("game_quit:error;unknown origin name");
               }
            }
            else if (op.equals("agent_score"))
            {
               String name = NimbusServer.connections.getNameBySession(session);
               if (name != null)
               {
                  PrisonersDilemmaAgent agent = NimbusServer.agents.get(name);
                  if (agent != null)
                  {
                     if (agent.games > 0)
                     {
                        session.getBasicRemote().sendText("agent_score:score;" + ((float)agent.outcomes / (float)agent.games) + "(" + agent.outcomes + "/" + agent.games + ")");
                     }
                     else
                     {
                        session.getBasicRemote().sendText("agent_score:score;0.0(0/0)");
                     }
                  }
                  else
                  {
                     session.getBasicRemote().sendText("agent_score:error;agent not found");
                  }
               }
               else
               {
                  session.getBasicRemote().sendText("agent_score:error;unknown agent name");
               }
            }
            else if (op.equals("agent_clear"))
            {
               String name = NimbusServer.connections.getNameBySession(session);
               if (name != null)
               {
                  PrisonersDilemmaAgent agent = NimbusServer.agents.get(name);
                  if (agent != null)
                  {
                     agent.outcomes = 0;
                     agent.games    = 0;
                     session.getBasicRemote().sendText("agent_clear:score;0.0(0/0)");
                  }
                  else
                  {
                     session.getBasicRemote().sendText("agent_clear:error;agent not found");
                  }
               }
               else
               {
                  session.getBasicRemote().sendText("agent_clear:error;unknown agent name");
               }
            }
            else if (op.equals("agent_scores"))
            {
               String agent_scores = "";
               for (Map.Entry<String, PrisonersDilemmaAgent> entry : NimbusServer.agents.entrySet())
               {
                  if (agent_scores.length() > 0)
                  {
                     agent_scores += ";";
                  }
                  PrisonersDilemmaAgent agent = entry.getValue();
                  if (agent.games > 0)
                  {
                     agent_scores += agent.name + "," + ((float)agent.outcomes / (float)agent.games) + "(" + agent.outcomes + "/" + agent.games + ")";
                  }
                  else
                  {
                     agent_scores += agent.name + ",0.0(0/0)";
                  }
               }
               session.getBasicRemote().sendText("agent_scores:scores;" + agent_scores);
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


   // Finish game.
   private void finishGame(PrisonersDilemmaGame game)
   {
      PrisonersDilemmaAgent agentA         = NimbusServer.agents.get(game.agentA);
      PrisonersDilemmaAgent agentB         = NimbusServer.agents.get(game.agentB);
      Session               agentA_session = NimbusServer.connections.getSessionByName(game.agentA);
      Session               agentB_session = NimbusServer.connections.getSessionByName(game.agentB);

      if ((agentA == null) || (agentB == null))
      {
         if (agentA_session != null)
         {
            try
            {
               if (agentA == null)
               {
                  agentA_session.getBasicRemote().sendText("game_outcome:error;agent not found " + game.agentA);
               }
            }
            catch (IOException e) {}
            try
            {
               if (agentB == null)
               {
                  agentA_session.getBasicRemote().sendText("game_outcome:error;agent not found " + game.agentB);
               }
            }
            catch (IOException e) {}
         }
         if (agentB_session != null)
         {
            try
            {
               if (agentA == null)
               {
                  agentB_session.getBasicRemote().sendText("game_outcome:error;agent not found " + game.agentA);
               }
            }
            catch (IOException e) {}
            try
            {
               if (agentB == null)
               {
                  agentB_session.getBasicRemote().sendText("game_outcome:error;agent not found " + game.agentB);
               }
            }
            catch (IOException e) {}
         }
      }
      else
      {
         if (game.agentAaction == PrisonersDilemmaGame.COOPERATE)
         {
            if (game.agentBaction == PrisonersDilemmaGame.COOPERATE)
            {
               agentA.outcomes += PrisonersDilemmaGame.BOTH_COOPERATE;
               agentA.games++;
               agentB.outcomes += PrisonersDilemmaGame.BOTH_COOPERATE;
               agentB.games++;
               if (agentA_session != null)
               {
                  try
                  {
                     agentA_session.getBasicRemote().sendText("game_outcome:complete;both cooperate, score = " + ((float)agentA.outcomes / (float)agentA.games) + "(" + agentA.outcomes + "/" + agentA.games + ")");
                  }
                  catch (IOException e) {}
               }
               if (agentB_session != null)
               {
                  try
                  {
                     agentB_session.getBasicRemote().sendText("game_outcome:complete;both cooperate, score = " + ((float)agentB.outcomes / (float)agentB.games) + "(" + agentB.outcomes + "/" + agentB.games + ")");
                  }
                  catch (IOException e) {}
               }
            }
            else
            {
               agentA.outcomes += PrisonersDilemmaGame.BETRAYED;
               agentA.games++;
               agentB.outcomes += PrisonersDilemmaGame.BETRAYER;
               agentB.games++;
               if (agentA_session != null)
               {
                  try
                  {
                     agentA_session.getBasicRemote().sendText("game_outcome:complete;betrayed, score = " + ((float)agentA.outcomes / (float)agentA.games) + "(" + agentA.outcomes + "/" + agentA.games + ")");
                  }
                  catch (IOException e) {}
               }
               if (agentB_session != null)
               {
                  try
                  {
                     agentB_session.getBasicRemote().sendText("game_outcome:complete;betrayer, score = " + ((float)agentB.outcomes / (float)agentB.games) + "(" + agentB.outcomes + "/" + agentB.games + ")");
                  }
                  catch (IOException e) {}
               }
            }
         }
         else
         {
            if (game.agentBaction == PrisonersDilemmaGame.COOPERATE)
            {
               agentA.outcomes += PrisonersDilemmaGame.BETRAYER;
               agentA.games++;
               agentB.outcomes += PrisonersDilemmaGame.BETRAYED;
               agentB.games++;
               if (agentA_session != null)
               {
                  try
                  {
                     agentA_session.getBasicRemote().sendText("game_outcome:complete;betrayer, score = " + ((float)agentA.outcomes / (float)agentA.games) + "(" + agentA.outcomes + "/" + agentA.games + ")");
                  }
                  catch (IOException e) {}
               }
               if (agentB_session != null)
               {
                  try
                  {
                     agentB_session.getBasicRemote().sendText("game_outcome:complete;betrayed, score = " + ((float)agentB.outcomes / (float)agentB.games) + "(" + agentB.outcomes + "/" + agentB.games + ")");
                  }
                  catch (IOException e) {}
               }
            }
            else
            {
               agentA.outcomes += PrisonersDilemmaGame.BOTH_BETRAYED;
               agentA.games++;
               agentB.outcomes += PrisonersDilemmaGame.BOTH_BETRAYED;
               agentB.games++;
               if (agentA_session != null)
               {
                  try
                  {
                     agentA_session.getBasicRemote().sendText("game_outcome:complete;both betrayed, score = " + ((float)agentA.outcomes / (float)agentA.games) + "(" + agentA.outcomes + "/" + agentA.games + ")");
                  }
                  catch (IOException e) {}
               }
               if (agentB_session != null)
               {
                  try
                  {
                     agentB_session.getBasicRemote().sendText("game_outcome:complete;both betrayed, score = " + ((float)agentB.outcomes / (float)agentB.games) + "(" + agentB.outcomes + "/" + agentB.games + ")");
                  }
                  catch (IOException e) {}
               }
            }
         }
      }
      NimbusServer.games.remove(game.agentA);
      NimbusServer.games.remove(game.agentB);
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
