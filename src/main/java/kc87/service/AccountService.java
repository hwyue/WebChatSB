package kc87.service;

import kc87.domain.Account;
import kc87.web.RegisterFormBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.validation.Errors;

public interface AccountService extends UserDetailsService {
   void createAccount(final Account account);

   Account prepareAccount(final Account account, final String password);

   Account prepareAccount(final RegisterFormBean formBean);

   Errors validateAccount(final Account account, final Errors errors);
}
