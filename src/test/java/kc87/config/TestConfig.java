package kc87.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

@Configuration
public class TestConfig {

   @Bean
   public SessionRegistry sessionRegistry() {
      return new SessionRegistryImpl();
   }

   /*
   @Bean(name = "authenticationManager")
   public AuthenticationManager authManager() throws Exception {
      return super.authenticationManagerBean();
   }*/
}
