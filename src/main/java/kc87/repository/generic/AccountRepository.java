package kc87.repository.generic;

import kc87.domain.Account;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@RepositoryRestResource(exported = true)
@SuppressWarnings("unused")
public interface AccountRepository extends PagingAndSortingRepository<Account, String> {
   Account findByUsernameIgnoreCase(String username);
}
