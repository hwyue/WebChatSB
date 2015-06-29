package kc87.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Controller
@RequestMapping(value = "/login")
public class LoginController {
   private static final Logger LOG = LogManager.getLogger(LoginController.class);
   private static final GrantedAuthority ADMIN_AUTHORITY = new SimpleGrantedAuthority("ROLE_ADMIN");

   @Autowired
   private SessionRegistry sessionRegistry;

   @Autowired
   AuthenticationManager authenticationManager;

   @RequestMapping(method = RequestMethod.GET)
   public ModelAndView form(final ModelAndView modelView, final HttpServletRequest request,
                            final HttpServletResponse response) {

      if (request.getCookies() == null) {
         response.addCookie(new Cookie("Enabled", "true"));
      }

      modelView.setViewName("login");
      modelView.addObject(new LoginFormBean());
      return modelView;
   }


   @RequestMapping(method = RequestMethod.POST)
   public String handleSubmit(final HttpServletRequest request, final HttpServletResponse response,
                              LoginFormBean formBean, BindingResult result) {

      if (request.getCookies() == null) {
         response.addCookie(new Cookie("Enabled", "true"));
         result.reject("error.cookies_disabled", "Cookies must be ON!");
         return "login";
      }

      try {
         Authentication authRequest = new UsernamePasswordAuthenticationToken(formBean.getUsername(),
                 formBean.getPassword());
         Authentication authResult = authenticationManager.authenticate(authRequest);
         SecurityContextHolder.getContext().setAuthentication(authResult);
         sessionRegistry.registerNewSession(request.getSession().getId(), authResult.getPrincipal());
         //sessionRegistry.registerNewSession(request.changeSessionId(),authResult.getPrincipal());
         Cookie removeCookie = new Cookie("Enabled", "true");
         removeCookie.setMaxAge(0);
         response.addCookie(removeCookie);
         return authResult.getAuthorities().contains(ADMIN_AUTHORITY) ? "redirect:intern" : "redirect:chat";
      } catch (BadCredentialsException e) {
         result.reject("error.wrong_credentials", "Wrong!");
         return "login";
      }
   }
}

