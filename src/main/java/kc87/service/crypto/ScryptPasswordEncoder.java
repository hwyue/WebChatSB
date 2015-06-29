package kc87.service.crypto;

import com.lambdaworks.crypto.SCryptUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ScryptPasswordEncoder implements PasswordEncoder {

   private static final Logger LOG = LogManager.getLogger(ScryptPasswordEncoder.class);
   private static final int PARAMETER_N = 65536;
   private static final int PARAMETER_R = 8;
   private static final int PARAMETER_P = 1;

   @Override
   public String encode(CharSequence rawPassword) {
      if (rawPassword == null) {
         return "";
      }
      return SCryptUtil.scrypt(rawPassword.toString(), PARAMETER_N, PARAMETER_R, PARAMETER_P);
   }

   @Override
   public boolean matches(CharSequence rawPassword, String encodedPassword) {
      if (rawPassword == null || encodedPassword == null) {
         return false;
      }
      return SCryptUtil.check(rawPassword.toString(), encodedPassword);
   }
}
