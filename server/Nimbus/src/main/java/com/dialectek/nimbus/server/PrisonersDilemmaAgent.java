// Prisoner's dilemma agent.

package com.dialectek.nimbus.server;

public class PrisonersDilemmaAgent
{
   public String name;
   public int    outcomes;
   public int    games;

   // Constructor.
   public PrisonersDilemmaAgent(String name)
   {
      this.name = name;
      outcomes  = 0;
      games     = 0;
   }


   // Add outcome.
   public void addOutcome(int outcome)
   {
      outcomes += outcome;
      games++;
   }
}
