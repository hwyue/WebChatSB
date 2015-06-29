package kc87.config;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import javax.servlet.SessionTrackingMode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

@Profile("tomcat")
@Configuration
public class TomcatConfig {
   private static final Logger LOG = LogManager.getLogger(TomcatConfig.class);

   @Autowired
   WebChatProperties webChatProperties;

   @Bean
   public EmbeddedServletContainerFactory servletContainer() {
      TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();

      factory.setSessionTimeout(webChatProperties.getSessionTimeout(), TimeUnit.SECONDS);
      factory.addInitializers(servletContext -> {
         servletContext.setSessionTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE));
         servletContext.getSessionCookieConfig().setName(webChatProperties.getSessionCookieName());
      });

      factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
         @Override
         public void customize(Connector connector) {
            connector.setPort(webChatProperties.getHttpPort());
            connector.setAttribute("server", "(oYo)");
            connector.setXpoweredBy(false);
         }
      });

      factory.addAdditionalTomcatConnectors(createSslConnector());

      AccessLogValve accessLogValve = new AccessLogValve();
      accessLogValve.setDirectory("/tmp");
      accessLogValve.setPattern("common");
      accessLogValve.setPrefix("tomcat");
      accessLogValve.setSuffix(".log");

      factory.addContextValves(accessLogValve);

      return factory;
   }


   private Connector createSslConnector() {
      Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
      Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();

      try {
         File keystore = getKeyStoreFile(webChatProperties.getKeystoreFile());
         connector.setAttribute("server", "(oYo)");
         connector.setXpoweredBy(false);
         connector.setScheme("https");
         connector.setSecure(true);
         connector.setPort(webChatProperties.getHttpsPort());
         protocol.setSSLEnabled(true);
         protocol.setKeystoreType(webChatProperties.getKeystoreType());
         protocol.setKeystoreFile(keystore.getAbsolutePath());
         protocol.setKeystorePass(webChatProperties.getKeystorePassword());
         return connector;
      } catch (IOException e) {
         throw new IllegalStateException(e);
      }
   }

   private File getKeyStoreFile(final String filePath) throws IOException {
      ClassPathResource resource = new ClassPathResource(filePath);
      try {
         return resource.getFile();
      } catch (Exception e) {
         File temp = File.createTempFile("ks", ".tmp");
         FileCopyUtils.copy(resource.getInputStream(), new FileOutputStream(temp));
         return temp;
      }
   }

}
