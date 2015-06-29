package kc87.service;

import kc87.config.WebChatProperties;
import kc87.domain.Account;
import kc87.repository.generic.AccountRepository;
import kc87.service.crypto.ScryptPasswordEncoder;
import kc87.service.validator.AccountValidator;
import kc87.web.RegisterFormBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.repository.init.Jackson2ResourceReader;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@SuppressWarnings("unused")
public class DefaultAccountService implements AccountService {
   private static final Logger LOG = LogManager.getLogger(DefaultAccountService.class);
   private static final PasswordEncoder PASSWORD_ENCODER = new ScryptPasswordEncoder();

   @Autowired
   WebChatProperties webChatProperties;

   @Autowired
   private AccountRepository accountRepository;

   @Autowired
   private AccountValidator accountValidator;

   @Override
   public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
      Account account = accountRepository.findByUsernameIgnoreCase(username);
      if (account != null) {
         List<GrantedAuthority> authorities = new ArrayList<>();
         for (String role : account.getRoles().split(",")) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
         }
         return new User(account.getUsername(), account.getPassword(), authorities);
      } else {
         throw new UsernameNotFoundException("User does not exist!");
      }
   }

   @Override
   public void createAccount(final Account account) {
      LOG.debug("Create account for: {}", account.getUsername());
      accountRepository.save(account);
   }

   @PostConstruct
   private void init() {
      LOG.debug("Init service");
      createTestAccounts();
   }

   @PreDestroy
   private void destroy() {
      LOG.debug("Destroy service");
   }

   public Errors validateAccount(final Account account, final Errors err) {
      Errors errors = (err == null) ? new BeanPropertyBindingResult(account, "Account") : err;
      accountValidator.validate(account, errors);
      return errors;
   }

   public Account prepareAccount(final RegisterFormBean formBean) {
      Account newAccount = new Account();
      newAccount.setFirstName(formBean.getFirstName());
      newAccount.setLastName(formBean.getLastName());
      newAccount.setEmail(formBean.getEmail());
      newAccount.setUsername(formBean.getUsername());
      return prepareAccount(newAccount, formBean.getPassword());
   }

   public Account prepareAccount(final Account account, String password) {
      try {
         account.setFirstName(account.getFirstName().trim());
         account.setLastName(account.getLastName().trim());
         account.setEmail(account.getEmail().trim());
         account.setUsername(account.getUsername().trim());
      } catch (NullPointerException e) {
         /*Catch potential null pointers caused by trim()*/
      }

      account.setCreated(new Date().getTime());
      account.setPassword(PASSWORD_ENCODER.encode(password));
      account.setRoles(account.getRoles() == null ? "USER" : account.getRoles());
      return account;
   }

   private void createTestAccounts() {
      Resource accountResource = new ClassPathResource(webChatProperties.getDbTestAccounts());
      Jackson2ResourceReader resourceReader = new Jackson2ResourceReader();
      try {
         Object accounts = resourceReader.readFrom(accountResource, this.getClass().getClassLoader());
         if (accounts instanceof List) {
            for (Object account : (List) accounts) {
               createTestAccount((Account) account);
            }
         } else {
            createTestAccount((Account) accounts);
         }
      } catch (Exception e) {
         LOG.error(e);
      }
   }

   private void createTestAccount(Account account) {
      prepareAccount(account, account.getPassword());
      Errors errors = validateAccount(account, null);
      if (!errors.hasErrors()) {
         createAccount(account);
      } else {
         for (ObjectError error : errors.getAllErrors()) {
            LOG.warn(error.getDefaultMessage());
         }
      }
   }

}
