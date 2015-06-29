# WebChatSB
Simple WebChat using Spring Boot

## Note
* This is free and open software developed for **research and educational** purposes only

## Some Features
* Stand-alone "just-run" app using Spring Boot
* XML-free Java config
* No parent POM's: all dependencies in one POM
* Support for Jetty 9 (default) or Tomcat 8 embedded servers
* WebSockets (JSR-356) based chat functionality 
* Access control by account registration and login using Spring Security/MVC
* Secure password hashing by Scrypt
* Admin Dashboard (w.i.p.): CRUD operations on Account table using REST (HAL) service:
  Spring Data REST on server and AngularJS (angular-hal) on client
* HSQLDB or MongoDB (default) for account persistence
* Hibernate validator for bean validation
* Thymeleaf template engine
* HTTP / HTTPS dual connector
* Optional redirect to secure channel
* HTTP request logging
* Log4j 2 logging
* EN/DE i18n support (w.i.p)


