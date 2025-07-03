package com.dialectek.nimbus.client;

import javax.websocket.*;
import java.io.IOException;
import java.util.logging.Logger;

@ClientEndpoint
public class WSClient {
   public static String id = "tom";

   private Logger logger = Logger.getLogger(this.getClass().getName());

   @OnOpen
   public void onOpen(Session session)
   {
      logger.info("Connected ... " + session.getId());
      try {
         session.getBasicRemote().sendText("id_to_name:" + id);
      }
      catch (IOException e) {
         throw new RuntimeException(e);
      }
   }


   @OnMessage
   public void onMessage(String message, Session session)
   {
      logger.info("Received ...." + message);
   }


   @OnClose
   public void onClose(Session session, CloseReason closeReason)
   {
      logger.info(String.format("Session %s close because of %s", session.getId(), closeReason));
   }
}
