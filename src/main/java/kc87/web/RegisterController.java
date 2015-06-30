package kc87.web;

import kc87.domain.Account;
import kc87.service.AccountService;
import kc87.service.SessionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
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

   private SessionService sessionService;

   private AccountService accountService;

   @Autowired
   public RegisterController(final SessionService sessionService, final AccountService accountService) {
      this.sessionService = sessionService;
      this.accountService = accountService;
   }

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

      // Check, if cookies are allowed:
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
         try {
            sessionService.authenticateUserSession(request.getSession().getId(),
                    formBean.getUsername(), formBean.getPassword());
         } catch (BadCredentialsException e) {
            LOG.error(e);
            return "redirect:login";
         }
         return "redirect:chat";
      }
      return "register";
   }
}
