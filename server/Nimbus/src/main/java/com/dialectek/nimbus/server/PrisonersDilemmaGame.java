// Prisoner's dilemma game.

package com.dialectek.nimbus.server;

public class PrisonersDilemmaGame
{
   // Game state.
   public static final int INVITE = 0;
   public static final int ACTIVE = 1;
   public int              state  = INVITE;

   // Agents.
   public String agentA;
   public String agentB;

   // Agent actions.
   public static final int PENDING   = 0;
   public static final int COOPERATE = 1;
   public static final int BETRAY    = 2;
   public int              agentAaction;
   public int              agentBaction;

   // Agent outcomes.
   public static int BOTH_COOPERATE = 3;
   public static int BOTH_BETRAY    = 1;
   public static int BETRAYED       = 0;
   public static int BETRAYER       = 5;
   public int        agentAoutcome;
   public int        agentBoutcome;

   // Constructor.
   public PrisonersDilemmaGame(String agentA, String agentB)
   {
      state        = INVITE;
      this.agentA  = agentA;
      this.agentB  = agentB;
      agentAaction = PENDING;
      agentBaction = PENDING;
   }


   // Join agent.
   public void join(String agent)
   {
      if ((state == INVITE) && agent.equals(agentB))
      {
         state = ACTIVE;
      }
   }


   // Agent action.
   public boolean act(String agent, int action)
   {
      if (state == ACTIVE)
      {
         if (agent.equals(agentA))
         {
            if (agentAaction == PENDING)
            {
               agentAaction = action;
            }
         }
         else if (agent.equals(agentB))
         {
            if (agentBaction == PENDING)
            {
               agentBaction = action;
            }
         }
         if ((agentAaction != PENDING) && (agentBaction != PENDING))
         {
            if (agentAaction == COOPERATE)
            {
               if (agentBaction == COOPERATE)
               {
                  agentAoutcome = agentBoutcome = BOTH_COOPERATE;
               }
               else
               {
                  agentAoutcome = BETRAYED;
                  agentBoutcome = BETRAYER;
               }
            }
            else
            {
               if (agentBaction == COOPERATE)
               {
                  agentAoutcome = BETRAYER;
                  agentBoutcome = BETRAYED;
               }
               else
               {
                  agentAoutcome = agentBoutcome = BOTH_BETRAY;
                  agentBoutcome = BETRAYER;
               }
            }
            return(true);
         }
      }
      return(false);
   }
}
