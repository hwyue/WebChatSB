package kc87.config;

import kc87.web.WsChatServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.validator.HibernateValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.ServletListenerRegistrationBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;


@Configuration
@SuppressWarnings("unused")
public class WebAppConfig extends WebMvcConfigurerAdapter {
   private static final Logger LOG = LogManager.getLogger(WebAppConfig.class);

   @Autowired
   WebChatProperties webChatProperties;

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

   @Bean
   public ServletListenerRegistrationBean httpSessionEventPublisher() {
      return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
   }

   @Bean
   public MessageSource messageSource() {
      ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
      messageSource.setBasename("classpath:i18n/messages");
      messageSource.setCacheSeconds(1);
      messageSource.setDefaultEncoding("UTF-8");
      return messageSource;
   }

   @Bean
   public LocalValidatorFactoryBean validator() {
      LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
      bean.setProviderClass(HibernateValidator.class);
      bean.setValidationMessageSource(messageSource());
      return bean;
   }

   @Override
   public Validator getValidator() {
      return validator();
   }

   @Override
   public void addViewControllers(ViewControllerRegistry registry) {
      registry.addViewController("/").setViewName("index");
      registry.addViewController("/index.*").setViewName("index");
      registry.addViewController("/chat").setViewName("chat");
      registry.addViewController("/intern").setViewName("dashboard");
   }
}
