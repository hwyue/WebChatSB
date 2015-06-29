package kc87.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(value = "webchat", ignoreUnknownFields = false)
public class WebChatProperties {

   private boolean httpLogging;
   private String httpLogfile;
   private int httpPort;
   private int httpsPort;
   private boolean httpForceTls;
   private String sessionCookieName;
   private int sessionTimeout;
   private String dbTestAccounts;
   private String jdbcUrl;
   private String mongodbUrl;
   private String keystoreFile;
   private String keystoreType;
   private String keystorePassword;
   private String keymanagerPassword;


   public boolean isHttpLogging() {
      return httpLogging;
   }

   public void setHttpLogging(boolean httpLogging) {
      this.httpLogging = httpLogging;
   }

   public String getHttpLogfile() {
      return httpLogfile;
   }

   public void setHttpLogfile(String httpLogfile) {
      this.httpLogfile = httpLogfile;
   }

   public int getHttpPort() {
      return httpPort;
   }

   public void setHttpPort(int httpPort) {
      this.httpPort = httpPort;
   }

   public int getHttpsPort() {
      return httpsPort;
   }

   public void setHttpsPort(int httpsPort) {
      this.httpsPort = httpsPort;
   }

   public boolean isHttpForceTls() {
      return httpForceTls;
   }

   public void setHttpForceTls(boolean httpForceTls) {
      this.httpForceTls = httpForceTls;
   }

   public String getSessionCookieName() {
      return sessionCookieName;
   }

   public void setSessionCookieName(String sessionCookieName) {
      this.sessionCookieName = sessionCookieName;
   }

   public int getSessionTimeout() {
      return sessionTimeout;
   }

   public void setSessionTimeout(int sessionTimeout) {
      this.sessionTimeout = sessionTimeout;
   }

   public String getDbTestAccounts() {
      return dbTestAccounts;
   }

   public void setDbTestAccounts(String dbTestAccounts) {
      this.dbTestAccounts = dbTestAccounts;
   }

   public String getJdbcUrl() {
      return jdbcUrl;
   }

   public void setJdbcUrl(String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
   }

   public String getMongodbUrl() {
      return mongodbUrl;
   }

   public void setMongodbUrl(String mongodbUrl) {
      this.mongodbUrl = mongodbUrl;
   }

   public String getKeystoreFile() {
      return keystoreFile;
   }

   public void setKeystoreFile(String keystoreFile) {
      this.keystoreFile = keystoreFile;
   }

   public String getKeystoreType() {
      return keystoreType;
   }

   public void setKeystoreType(String keystoreType) {
      this.keystoreType = keystoreType;
   }

   public String getKeystorePassword() {
      return keystorePassword;
   }

   public void setKeystorePassword(String keystorePassword) {
      this.keystorePassword = keystorePassword;
   }

   public String getKeymanagerPassword() {
      return keymanagerPassword;
   }

   public void setKeymanagerPassword(String keymanagerPassword) {
      this.keymanagerPassword = keymanagerPassword;
   }
}
