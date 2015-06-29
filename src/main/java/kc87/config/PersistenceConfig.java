package kc87.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;


@Configuration
public class PersistenceConfig {
   private static final Logger LOG = LogManager.getLogger(PersistenceConfig.class);


   @Profile("generic_mongo")
   @Configuration
   @EnableJpaRepositories(basePackages = {"kc87.repository.jpa"}, enableDefaultTransactions = true)
   public static class JpaConfig {
      private static final Logger LOG = LogManager.getLogger(PersistenceConfig.class);

      @Autowired
      WebChatProperties webChatProperties;

      @Bean
      public DataSource dataSource() {
         return new PersistenceJpaConfig(webChatProperties).dataSource();
      }

      @Bean
      public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
         return new PersistenceJpaConfig(webChatProperties).entityManagerFactory(dataSource());
      }
   }


   @Profile("generic_jpa")
   @Configuration
   @EnableMongoRepositories(basePackages = {"kc87.repository.mongo"})
   public static class MongoConfig {
      @Autowired
      WebChatProperties webChatProperties;

      @Bean
      public MongoDbFactory mongoDbFactory() throws Exception {
         return new PersistenceMongoConfig(webChatProperties).mongoDbFactory();
      }

      @Bean
      public MongoTemplate mongoTemplate() throws Exception {
         return new PersistenceMongoConfig(webChatProperties).mongoTemplate(mongoDbFactory());
      }
   }


   @Profile("generic_jpa")
   @Configuration
   @EnableJpaRepositories(basePackages = {"kc87.repository.jpa", "kc87.repository.generic"},
           enableDefaultTransactions = true)
   public static class GenericJpaConfig {
      @Autowired
      WebChatProperties webChatProperties;

      @Bean
      public DataSource dataSource() {
         return new PersistenceJpaConfig(webChatProperties).dataSource();
      }

      @Bean
      public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
         return new PersistenceJpaConfig(webChatProperties).entityManagerFactory(dataSource());
      }
   }

   @Profile("generic_mongo")
   @Configuration
   @EnableMongoRepositories(basePackages = {"kc87.repository.mongo", "kc87.repository.generic"})
   public static class GenericMongoConfig {
      @Autowired
      WebChatProperties webChatProperties;

      @Bean
      public MongoDbFactory mongoDbFactory() throws Exception {
         return new PersistenceMongoConfig(webChatProperties).mongoDbFactory();
      }

      @Bean
      public MongoTemplate mongoTemplate() throws Exception {
         return new PersistenceMongoConfig(webChatProperties).mongoTemplate(mongoDbFactory());
      }
   }
}
