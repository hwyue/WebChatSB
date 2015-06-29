package kc87.repository;


import kc87.domain.Account;
import kc87.service.AccountService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
@SuppressWarnings("unused")
public class AccountEventHandler {
   private static final Logger LOG = LogManager.getLogger(AccountEventHandler.class);

   @Autowired
   private AccountService accountService;

   @HandleBeforeCreate
   public void handleAccountCreate(Account account) {
      LOG.debug("HandleBeforeCreate");
      accountService.prepareAccount(account, account.getPassword());
   }
}
