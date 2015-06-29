package kc87.repository.jpa;

import kc87.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@NoRepositoryBean
@RepositoryRestResource(exported = true)
@SuppressWarnings("unused")
public interface AccountRepository extends JpaRepository<Account, String> {
   Account findByUsernameIgnoreCase(String username);
}
