package kc87.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.session.SessionFixationProtectionEvent;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

@Service
@SuppressWarnings("unused")
public class SessionService implements ApplicationListener<ApplicationEvent> {
   private static final Logger LOG = LogManager.getLogger(SessionService.class);
   private static List<BiConsumer<String, String>> callbacks = new CopyOnWriteArrayList<>();
   private static Map<String, HttpSession> sessionList = new ConcurrentHashMap<>(128);

   @Autowired
   private SessionRegistry sessionRegistry;


   @Override
   public void onApplicationEvent(ApplicationEvent event) {

      if (event instanceof HttpSessionCreatedEvent) {
         HttpSessionCreatedEvent sessionCreated = (HttpSessionCreatedEvent) event;
         HttpSession session = sessionCreated.getSession();
         sessionList.put(session.getId(), session);
         LOG.debug("HTTP session created: " + session.getId());
         return;
      }


      if (event instanceof HttpSessionDestroyedEvent) {
         HttpSessionDestroyedEvent sessionDestroyed = (HttpSessionDestroyedEvent) event;
         LOG.debug("HTTP session destroyed: " + sessionDestroyed.getId());
         sessionList.remove(sessionDestroyed.getId());
         for (SecurityContext context : sessionDestroyed.getSecurityContexts()) {
            User user = (User) context.getAuthentication().getPrincipal();
            for (BiConsumer<String, String> consumer : callbacks) {
               consumer.accept(sessionDestroyed.getId(), user.getUsername());
            }
         }
         return;
      }


      if (event instanceof SessionFixationProtectionEvent) {
         SessionFixationProtectionEvent sessionFixation = (SessionFixationProtectionEvent) event;
         LOG.debug("Session id changed: " + sessionFixation.getOldSessionId() + " => " + sessionFixation.getNewSessionId());
         if (sessionList.containsKey(sessionFixation.getOldSessionId())) {
            HttpSession session = sessionList.get(sessionFixation.getOldSessionId());
            sessionList.remove(sessionFixation.getOldSessionId());
            sessionList.put(sessionFixation.getNewSessionId(), session);
         }
         return;
      }


      if (event instanceof AuthenticationSuccessEvent) {
         AuthenticationSuccessEvent authenticationSuccess = (AuthenticationSuccessEvent) event;
         User newUser = (User) authenticationSuccess.getAuthentication().getPrincipal();
         LOG.debug("Authentication successful for: " + newUser.getUsername());

         for (String id : sessionList.keySet()) {
            if (sessionRegistry.getSessionInformation(id) != null) {
               User user = (User) sessionRegistry.getSessionInformation(id).getPrincipal();
               LOG.debug("Session found for: " + user.getUsername());
               if (user.getUsername().equals(newUser.getUsername())) {
                  LOG.debug("Expire pending session for: " + user.getUsername() + " / " + id);
                  sessionRegistry.getSessionInformation(id).expireNow();
                  sessionList.get(id).invalidate();
                  sessionList.remove(id);
                  return;
               }
            }
         }
      }
   }


   public synchronized void addOnSessionDestroyedListener(BiConsumer<String, String> callback) {
      callbacks.add(callback);
   }

   public synchronized void removeOnSessionDestroyedListener(BiConsumer<String, String> callback) {
      LOG.debug("removeOnSessionDestroyedListener()");
      callbacks.remove(callback);
   }

}
