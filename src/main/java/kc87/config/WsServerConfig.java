package kc87.config;


import kc87.web.WsChatServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WsServerConfig {

   @Autowired
   private WebChatProperties webChatProperties;

   @Bean
   public WsChatServer wsChatServer() {
      WsChatServer chatServer = new WsChatServer();
      chatServer.setChatSessionTimeout(webChatProperties.getSessionTimeout());
      return chatServer;
   }

   @Bean
   public ServerEndpointExporter serverEndpointExporter() {
      return new ServerEndpointExporter();
   }
}
