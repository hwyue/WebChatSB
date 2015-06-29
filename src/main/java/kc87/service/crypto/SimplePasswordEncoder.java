package kc87.service.crypto;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class SimplePasswordEncoder implements PasswordEncoder {

   private static final Logger LOG = LogManager.getLogger(SimplePasswordEncoder.class);
   private static final String HASH_ALGO = "SHA-256";
   private static final int SALT_SIZE = 4;


   public static boolean isPasswordCorrect(final String password, final String hash) {
      LOG.debug("PW:" + password + " hash:" + hash);
      String salt = hash.split(":")[1];
      return hashString(password + salt).equals(hash.split(":")[0]);
   }

   public static String encryptPassword(final String password) {
      String result;
      Random rnd = new Random();
      StringBuilder saltStringBuilder = new StringBuilder();

      // Generate SALT_SIZE byte random salt:
      for (int i = 0; i < SALT_SIZE; i++) {
         saltStringBuilder.append(Integer.toString((rnd.nextInt(255) & 0xff), 16));
      }

      //TODO: Get rid of the colon and use SALT_SIZE instead
      result = hashString(password + saltStringBuilder.toString()) + ":" + saltStringBuilder.toString();

      return result;
   }


   private static String hashString(final String str) {
      MessageDigest md;

      try {
         md = MessageDigest.getInstance(HASH_ALGO);
      } catch (NoSuchAlgorithmException e) {
         LOG.fatal(e);
         throw new RuntimeException(e);
      }

      md.update(str.getBytes());
      byte[] dataBytes = md.digest();

      StringBuilder sb = new StringBuilder();
      for (byte b : dataBytes) {
         sb.append(Integer.toString((b & 0xff), 16));
      }

      return sb.toString();
   }

   @Override
   public String encode(CharSequence rawPassword) {
      return SimplePasswordEncoder.encryptPassword(rawPassword.toString());
   }

   @Override
   public boolean matches(CharSequence rawPassword, String encodedPassword) {
      return SimplePasswordEncoder.isPasswordCorrect(rawPassword.toString(), encodedPassword);
   }
}
