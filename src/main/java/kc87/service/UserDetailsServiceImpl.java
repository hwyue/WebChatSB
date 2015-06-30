package kc87.service;


import kc87.domain.Account;
import kc87.repository.generic.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@SuppressWarnings("unused")
public class UserDetailsServiceImpl implements UserDetailsService {

   private AccountRepository accountRepository;

   @Autowired
   public UserDetailsServiceImpl(final AccountRepository accountRepository) {
      this.accountRepository = accountRepository;
   }

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
}
