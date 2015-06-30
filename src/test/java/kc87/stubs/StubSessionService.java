package kc87.stubs;


import kc87.service.SessionService;
import org.springframework.context.ApplicationEvent;
import org.springframework.security.core.Authentication;

import java.util.function.BiConsumer;

public class StubSessionService implements SessionService {

   @Override
   public Authentication authenticateUserSession(String sessionId, String username, String password) {
      return null;
   }

   @Override
   public void addOnSessionDestroyedListener(BiConsumer<String, String> callback) {
      throw new UnsupportedOperationException("Unimplemented method stub");
   }

   @Override
   public void removeOnSessionDestroyedListener(BiConsumer<String, String> callback) {
      throw new UnsupportedOperationException("Unimplemented method stub");
   }

   @Override
   public void onApplicationEvent(ApplicationEvent event) {
      throw new UnsupportedOperationException("Unimplemented method stub");
   }
}
