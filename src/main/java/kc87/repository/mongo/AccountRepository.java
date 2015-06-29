package kc87.repository.mongo;

import kc87.domain.Account;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

;


@NoRepositoryBean
@RepositoryRestResource(exported = true)
@SuppressWarnings("unused")
public interface AccountRepository extends MongoRepository<Account, String> {
   Account findByUsernameIgnoreCase(String username);
}
