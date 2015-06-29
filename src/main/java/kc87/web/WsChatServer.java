package kc87.web;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import kc87.domain.ChatLog;
import kc87.repository.mongo.ChatLogRepository;
import kc87.service.SessionService;
import kc87.web.protocol.ChatMsg;
import kc87.web.protocol.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.HandshakeResponse;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;


@ServerEndpoint(value = "/ws", configurator = WsChatServer.Config.class)
@SuppressWarnings("unused")
public class WsChatServer implements ApplicationContextAware {
   private static final Logger LOG = LogManager.getLogger(WsChatServer.class);
   private static final int IDLE_TIMEOUT_SEC = 60;
   private static final int WS_SESSION_TIMEOUT_SEC = 100;
   private static final String[] PEER_COLORS = {"#38F", "#f00", "#ff0", "#f08", "#0ff",
           "#888", "#8ff", "#f80", "#ff4", "#fff"};
   private static final int PEER_COLOR_NB = PEER_COLORS.length;
   private static final AbstractMap<String, String> userColorMap = new ConcurrentHashMap<>();
   private static AtomicInteger usersLoggedIn = new AtomicInteger(0);
   private static ApplicationContext appContext = null;
   private static SessionService sessionService = null;
   private static ChatLogRepository chatLogRepository = null;
   private static int chatSessionTimeout = WS_SESSION_TIMEOUT_SEC;
   private UsernamePasswordAuthenticationToken authToken = null;
   private Session thisSession = null;
   private HttpSession httpSession = null;
   private long lastActivityTime = 0;
   private int defaultSessionTimeout = 0;
   private final BiConsumer<String, String> callback = this::sessionDestroyed;
   private boolean isHttpSessionValid = false;
   private String httpSessionId = null;

   public void setChatSessionTimeout(final int timeout) {
      chatSessionTimeout = timeout;
   }

   @PostConstruct
   private void init() {
      LOG.debug("@PostConstruct");
      sessionService = appContext.getBean(SessionService.class);
      chatLogRepository = appContext.getBean(ChatLogRepository.class);
   }

   @OnOpen
   public void onOpen(Session session) {
      LOG.debug("@OnOpen: " + session.getId());
      thisSession = session;

      if (thisSession.getUserPrincipal() != null) {
         authToken = (UsernamePasswordAuthenticationToken) thisSession.getUserPrincipal();
         if (authToken.isAuthenticated()) {
            isHttpSessionValid = true;
            httpSession = (HttpSession) thisSession.getUserProperties().get("httpSession");
            httpSessionId = httpSession.getId();
            LOG.debug("HTTP Session id: " + httpSessionId);
            if (httpSession.getAttribute("CHAT_OPEN") != null) {
               activeClose(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "ALREADY_OPEN"));
            } else {
               httpSession.setAttribute("CHAT_OPEN", true);
            }
            return;
         }
      }

      activeClose(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthorized"));
   }

   @OnMessage
   public void onTextMsg(String jsonStr) {
      Gson gson = new GsonBuilder().serializeNulls().create();

      try {
         jsonStr = jsonStr.trim();
         LOG.debug("Rcv.:" + jsonStr + " from: " + thisSession.getId());
         Message clientMsg = gson.fromJson(jsonStr, Message.class);

         if (clientMsg.TYPE.equals("PING")) {
            sendPong();
         }

         if (clientMsg.TYPE.equals("JOIN")) {
            joinChat();
         }

         if (clientMsg.TYPE.equals("CHAT")) {
            handleChat(clientMsg);
         }

      } catch (JsonSyntaxException e) {
         LOG.error("@OnMessage: " + e);
      }
   }

   @OnClose
   public void onClose() {
      LOG.debug("@OnClose: " + thisSession.getId());
      if (isHttpSessionValid) {
         httpSession.removeAttribute("CHAT_OPEN");
      }
      unjoinChat();
   }

   @OnError
   public void onError(Throwable throwable) {
      LOG.error("@OnError: " + throwable);
   }

   @Override
   public void setApplicationContext(ApplicationContext ctx) throws BeansException {
      appContext = ctx;
   }

   private void joinChat() {
      String userColor;

      sessionService.addOnSessionDestroyedListener(callback);

      defaultSessionTimeout = httpSession.getMaxInactiveInterval();
      httpSession.setMaxInactiveInterval(0);
      lastActivityTime = System.currentTimeMillis();

      String username = ((User) authToken.getPrincipal()).getUsername();
      LOG.debug("joinChat() user: " + username);

      thisSession.getUserProperties().clear();
      thisSession.setMaxIdleTimeout(IDLE_TIMEOUT_SEC * 1000);
      thisSession.getUserProperties().put("USER", username);

      int userNb = usersLoggedIn.incrementAndGet();
      // If a user is active more than once, give him the same color:
      if (userColorMap.containsKey(username)) {
         userColor = userColorMap.get(username);
      } else {
         userColor = PEER_COLORS[userNb % PEER_COLOR_NB];
         userColorMap.put(username, userColor);
      }

      thisSession.getUserProperties().put("COLOR", userColor);

      Message joinMsg = new Message();
      joinMsg.TYPE = "JOIN";
      joinMsg.SUBTYPE = "JOIN";
      joinMsg.USER_LIST = buildUserList(true);
      joinMsg.STATS_MSG = userNb + " User" + (userNb > 1 ? "s " : " ") + "online!";

      sendMessage(joinMsg);

      Message infoMsg = new Message();
      infoMsg.TYPE = "INFO";
      infoMsg.SUBTYPE = "JOIN";
      infoMsg.INFO_MSG = username + " has entered the building";
      infoMsg.STATS_MSG = userNb + " User" + (userNb > 1 ? "s " : " ") + "online!";
      infoMsg.USER_LIST = buildUserList(true);

      broadcastMessage(infoMsg, false);
   }


   private void unjoinChat() {
      if (thisSession.getUserProperties().containsKey("USER")) {
         LOG.debug("unjoinChat(): " + thisSession.getUserProperties().get("USER"));

         sessionService.removeOnSessionDestroyedListener(callback);

         if (isHttpSessionValid) {
            int sessionIdleTime = (int) ((System.currentTimeMillis() - httpSession.getLastAccessedTime()) / 1000);
            LOG.debug("Max idle timeout: " + (sessionIdleTime + defaultSessionTimeout));
            httpSession.setMaxInactiveInterval(sessionIdleTime + defaultSessionTimeout);
         }

         int userNb = usersLoggedIn.decrementAndGet();

         Message infoMsg = new Message();

         infoMsg.TYPE = "INFO";
         infoMsg.SUBTYPE = "JOIN";
         infoMsg.INFO_MSG = thisSession.getUserProperties().get("USER") + " has left the building";
         infoMsg.STATS_MSG = userNb + " User" + (userNb > 1 ? "s " : " ") + "online!";
         infoMsg.USER_LIST = buildUserList(false);

         thisSession.getUserProperties().clear();

         broadcastMessage(infoMsg, false);
      }
   }


   private void handleChat(final Message clientMsg) {
      Message broadcastMsg;

      if (clientMsg.SUBTYPE.equals("MSG")) {

         lastActivityTime = System.currentTimeMillis();

         ChatMsg chatMsg = new ChatMsg();
         broadcastMsg = clientMsg;
         // You can't trust nobody ;)
         chatMsg.MSG = clientMsg.CHAT_MSG.MSG.replace("<", "&lt;").replace("&", "&amp;");
         chatMsg.COLOR = (String) thisSession.getUserProperties().get("COLOR");
         chatMsg.FROM = (String) thisSession.getUserProperties().get("USER");

         ChatLog logMsg = new ChatLog();
         logMsg.setDate(new Date(lastActivityTime));
         logMsg.setUser(chatMsg.FROM);
         logMsg.setMessage(chatMsg.MSG);

         try {
            chatLogRepository.insert(logMsg);
         } catch (Exception e) {
            LOG.error(e.getMessage());
         }

         broadcastMsg.CHAT_MSG = chatMsg;
         broadcastMessage(broadcastMsg, true);
      }
   }


   private void sendPong() {
      if (System.currentTimeMillis() - lastActivityTime > chatSessionTimeout * 1000) {
         httpSession.invalidate();
         return;
      }
      Message pongMsg = new Message();
      pongMsg.TYPE = "PONG";
      sendMessage(pongMsg);
   }


   private void activeClose(CloseReason reason) {
      try {
         if (thisSession.isOpen()) {
            LOG.debug("Closing connection to peer: " + thisSession.getId());
            thisSession.close(reason);
         }
      } catch (IOException e) {
         LOG.error(e);
      }
   }


   private String[] buildUserList(final boolean includeThis) {
      List<String> userList = new ArrayList<>();

      LOG.debug("buildUserList(): " + thisSession.getOpenSessions().size());

      for (Session session : thisSession.getOpenSessions()) {
         if (!includeThis && thisSession.equals(session)) {
            continue;
         }
         String userName = (String) session.getUserProperties().get("USER");
         String userColor = (String) session.getUserProperties().get("COLOR");
         userList.add(userColor + "*" + userName);
      }

      return (userList.size() == 0) ? null : userList.toArray(new String[userList.size()]);
   }

   private void sendMessage(final Message serverMsg) {
      final Gson gson = new Gson();

      try {
         final String jsonStr = gson.toJson(serverMsg);
         if (thisSession.isOpen()) {
            LOG.debug("Send: " + jsonStr + " to: " + thisSession.getId());
            thisSession.getBasicRemote().sendText(jsonStr);
         }
      } catch (Exception e) {
         LOG.error(e);
      }
   }

   private void broadcastMessage(final Message serverMsg, final boolean includeThis) {
      final Gson gson = new Gson();

      try {
         final String jsonStr = gson.toJson(serverMsg);

         for (Session session : thisSession.getOpenSessions()) {
            if (!includeThis && thisSession.equals(session)) {
               continue;
            }
            session.getAsyncRemote().sendText(jsonStr);
         }
      } catch (Exception e) {
         LOG.error(e);
      }
   }

   private void sessionDestroyed(final String id, final String username) {
      LOG.debug("sessionDestroyed(): " + username + "/" + id);
      if (id.equals(httpSessionId)) {
         isHttpSessionValid = false;
         unjoinChat();
         new Thread(() -> {
            try {
               Thread.sleep(100);
            } catch (InterruptedException e) {
               /* IGNORED*/
            }
            activeClose(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "SESSION_DESTROYED"));
         }).start();
      }
   }


   public static class Config extends ServerEndpointConfig.Configurator {
      @Override
      public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
         super.modifyHandshake(sec, request, response);
         HttpSession httpSession = (HttpSession) request.getHttpSession();
         sec.getUserProperties().put("httpSession", httpSession);
      }
   }

}
