package kc87.config;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.servlet.SessionTrackingMode;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

@Profile("jetty")
@Configuration
public class JettyConfig {

   @Autowired
   WebChatProperties webChatProperties;

   @Bean
   public EmbeddedServletContainerFactory servletContainerJetty() {
      JettyEmbeddedServletContainerFactory factory = new JettyEmbeddedServletContainerFactory();

      factory.setSessionTimeout(webChatProperties.getSessionTimeout(), TimeUnit.SECONDS);

      factory.addInitializers(servletContext -> {
         servletContext.setSessionTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE));
         servletContext.getSessionCookieConfig().setName(webChatProperties.getSessionCookieName());
      });

      factory.addServerCustomizers(server -> {
         if (webChatProperties.isHttpLogging()) {
            setupRequestLogging(server);
         }
         ServerConnector defaultConnector = (ServerConnector) (server.getConnectors())[0];
         defaultConnector.setPort(webChatProperties.getHttpPort());
         addHttpsConnector(server);
         // Disable http server header for all connectors
         for (Connector connector : server.getConnectors()) {
            for (ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
               if (connectionFactory instanceof HttpConnectionFactory) {
                  HttpConfiguration httpConfiguration = ((HttpConnectionFactory) connectionFactory)
                          .getHttpConfiguration();
                  httpConfiguration.setSendServerVersion(false);
               }
            }
         }
      });

      return factory;
   }

   private void setupRequestLogging(final Server server) {
      HandlerCollection handlers = new HandlerCollection();

      for (Handler handler : server.getHandlers()) {
         handlers.addHandler(handler);
      }

      RequestLogHandler requestLogHandler = new RequestLogHandler();

      NCSARequestLog requestLog = new NCSARequestLog(webChatProperties.getHttpLogfile());
      requestLog.setRetainDays(30);
      requestLog.setAppend(true);
      requestLog.setExtended(false);
      requestLog.setLogTimeZone("CET");
      requestLogHandler.setRequestLog(requestLog);

      handlers.addHandler(requestLogHandler);
      server.setHandler(handlers);
   }


   private void addHttpsConnector(final Server server) {
      SslContextFactory sslContextFactory = new SslContextFactory();
      sslContextFactory.setKeyStorePassword(webChatProperties.getKeystorePassword());
      sslContextFactory.setKeyManagerPassword(webChatProperties.getKeymanagerPassword());
      sslContextFactory.setKeyStorePath(JettyConfig.class.getResource(webChatProperties.getKeystoreFile()).toExternalForm());
      sslContextFactory.setKeyStoreType(webChatProperties.getKeystoreType());
      sslContextFactory.setNeedClientAuth(false);

      HttpConfiguration httpsConfiguration = new HttpConfiguration();
      httpsConfiguration.addCustomizer(new SecureRequestCustomizer());

      ServerConnector httpsConnector = new ServerConnector(server,
              new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
              new HttpConnectionFactory(httpsConfiguration));
      httpsConnector.setPort(webChatProperties.getHttpsPort());
      server.addConnector(httpsConnector);
   }

}
