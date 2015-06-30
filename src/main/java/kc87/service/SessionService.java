package kc87.service;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;

import java.util.function.BiConsumer;

public interface SessionService extends ApplicationListener<ApplicationEvent> {

   Authentication authenticateUserSession(String sessionId, String username,
                                          String password);

   void addOnSessionDestroyedListener(BiConsumer<String, String> callback);

   void removeOnSessionDestroyedListener(BiConsumer<String, String> callback);
}
