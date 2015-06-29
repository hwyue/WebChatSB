package kc87;

import org.joda.time.DateTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Date;


@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Main {

   @SuppressWarnings("unused")
   public static String getCurrentTime() {
      return new DateTime(new Date()).toString("dd.MM.yyyy HH:mm:ss");
   }

   public static void main(String[] args) {
      SpringApplication springApplication = new SpringApplication(Main.class);
      springApplication.setWebEnvironment(true);
      springApplication.run(args);
   }
}
