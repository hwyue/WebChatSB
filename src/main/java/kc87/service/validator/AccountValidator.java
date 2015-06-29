package kc87.service.validator;

import kc87.domain.Account;
import kc87.repository.generic.AccountRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;


@Component
public class AccountValidator extends SpringValidatorAdapter {
   private static final Logger LOG = LogManager.getLogger(AccountValidator.class);
   private AccountRepository accountRepository;

   @Autowired
   public AccountValidator(final Validator validator, final AccountRepository accountRepository) {
      super(validator);
      this.accountRepository = accountRepository;
   }

   @Override
   public boolean supports(Class<?> aClass) {
      return Account.class.equals(aClass);
   }

   @Override
   public void validate(Object obj, Errors errors) {
      Account account = (Account) obj;

      // Default validation
      super.validate(account, errors);

      // Custom validation
      if (!errors.hasErrors()) {
         Account dbAccount = accountRepository.findByUsernameIgnoreCase(account.getUsername());
         if (dbAccount != null && !dbAccount.getId().equals(account.getId())) {
            LOG.debug("Reject: " + account.toString());
            errors.rejectValue("username", "error.username_taken", "Username already taken!");
         }
      }
   }

   @Override
   public ExecutableValidator forExecutables() {
      throw new UnsupportedOperationException("forExecutables()");
   }
}
