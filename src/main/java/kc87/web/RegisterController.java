package kc87.web;

import kc87.domain.Account;
import kc87.service.AccountService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping(value = "/register")
public class RegisterController {
   private static final Logger LOG = LogManager.getLogger(RegisterController.class);

   @Autowired
   private SessionRegistry sessionRegistry;

   @Autowired
   private AccountService accountService;

   @RequestMapping(method = RequestMethod.GET)
   public ModelAndView form(final ModelAndView modelView, final HttpServletRequest request,
                            final HttpServletResponse response) {
      if (request.getCookies() == null) {
         response.addCookie(new Cookie("Enabled", "true"));
      }
      modelView.setViewName("register");
      modelView.addObject(new RegisterFormBean());
      return modelView;
   }

   @RequestMapping(method = RequestMethod.POST)
   public String handleSubmit(final HttpServletRequest request, final HttpServletResponse response,
                              RegisterFormBean formBean, BindingResult result) {

      if (request.getCookies() == null) {
         response.addCookie(new Cookie("Enabled", "true"));
         result.reject("error.cookies_disabled", "Cookies must be ON!");
         return "register";
      } else {
         Cookie removeCookie = new Cookie("Enabled", "true");
         removeCookie.setMaxAge(0);
         response.addCookie(removeCookie);
      }

      Account newAccount = accountService.prepareAccount(formBean);
      accountService.validateAccount(newAccount, result);
      if (!result.hasErrors()) {
         accountService.createAccount(newAccount);
         // After successful registration, login the user automatically
         autoLogin(formBean.getUsername(), request.getSession().getId());
         return "redirect:chat";
      }
      return "register";
   }

   private void autoLogin(final String username, final String sessionId) {
      try {
         UserDetails user = accountService.loadUserByUsername(username);
         Authentication authToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
         SecurityContextHolder.getContext().setAuthentication(authToken);
         sessionRegistry.registerNewSession(sessionId, user);
      } catch (Exception e) {
         LOG.error(e);
      }
   }
}
