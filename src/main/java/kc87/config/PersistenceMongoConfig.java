package kc87.config;

import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;


public class PersistenceMongoConfig {

   WebChatProperties webChatProperties;

   public PersistenceMongoConfig(final WebChatProperties properties) {
      webChatProperties = properties;
   }

   public MongoDbFactory mongoDbFactory() throws Exception {
      MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
      builder.connectTimeout(1000);
      MongoClientURI mongoClientURI = new MongoClientURI(webChatProperties.getMongodbUrl(), builder);
      return new SimpleMongoDbFactory(mongoClientURI);
   }

   public MongoTemplate mongoTemplate(final MongoDbFactory mongoDbFactory) throws Exception {
      return new MongoTemplate(mongoDbFactory);
   }
}
