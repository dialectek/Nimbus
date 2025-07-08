// Name to session bidirectional map.

package com.dialectek.nimbus.server;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.websocket.Session;

public class NameToSessionMap
{
   private final Map<String, String>  nameToSessionIdMap    = new TreeMap<>();
   private final Map<String, String>  sessionIdToNameMap    = new TreeMap<>();
   private final Map<String, Session> sessionIdToSessionMap = new TreeMap<>();

   // Put a name-session pair into the bidirectional map.
   public void put(String name, Session session)
   {
      String  sessionId  = session.getId();
      Session oldSession = getSessionByName(name);

      if ((oldSession != null) && (oldSession.getId() != sessionId))
      {
         try
         {
            oldSession.close();
         }
         catch (IOException e) {}
      }
      removeByName(name);
      removeBySession(session);
      nameToSessionIdMap.put(name, sessionId);
      sessionIdToNameMap.put(sessionId, name);
      sessionIdToSessionMap.put(sessionId, session);
   }


   // Get a session based on the name.
   public Session getSessionByName(String name)
   {
      String sessionId = nameToSessionIdMap.get(name);

      if (sessionId != null)
      {
         return(sessionIdToSessionMap.get(sessionId));
      }
      else
      {
         return(null);
      }
   }


   // Get a name based on the session
   public String getNameBySession(Session session)
   {
      return(sessionIdToNameMap.get(session.getId()));
   }


   // Check if a name exists in the map.
   public boolean containsName(String name)
   {
      return(nameToSessionIdMap.containsKey(name));
   }


   // Check if a session exists in the map.
   public boolean containsSession(Session session)
   {
      return(sessionIdToNameMap.containsKey(session.getId()));
   }


   // Remove a name-session pair based on the name.
   public void removeByName(String name)
   {
      String sessionId = null;

      if ((sessionId = nameToSessionIdMap.remove(name)) != null)
      {
         sessionIdToNameMap.remove(sessionId);
         sessionIdToSessionMap.remove(sessionId);
      }
   }


   // Remove a name-session pair based on the session.
   public void removeBySession(Session session)
   {
      String name      = null;
      String sessionId = session.getId();

      if ((name = sessionIdToNameMap.remove(sessionId)) != null)
      {
         nameToSessionIdMap.remove(name);
         sessionIdToSessionMap.remove(sessionId);
      }
   }


   // Remove all name-session pairs from the bidirectional map.
   public void removeAll()
   {
      nameToSessionIdMap.clear();
      sessionIdToNameMap.clear();
      sessionIdToSessionMap.clear();
   }


   // Get a set of all names in the map.
   public Set<String> getAllNames()
   {
      return(nameToSessionIdMap.keySet());
   }


   // Get a collection of all sessions in the map.
   public Collection<Session> getAllSessions()
   {
      return(sessionIdToSessionMap.values());
   }
}
