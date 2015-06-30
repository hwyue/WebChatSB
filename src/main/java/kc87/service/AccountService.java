package kc87.service;

import kc87.domain.Account;
import kc87.web.RegisterFormBean;
import org.springframework.validation.Errors;

public interface AccountService {
   void createAccount(final Account account);

   Account prepareAccount(final Account account, final String password);

   Account prepareAccount(final RegisterFormBean formBean);

   Errors validateAccount(final Account account, final Errors errors);
}
