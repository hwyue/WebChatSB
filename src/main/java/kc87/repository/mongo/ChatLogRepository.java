package kc87.repository.mongo;

import kc87.domain.ChatLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@RepositoryRestResource(exported = false)
@SuppressWarnings("unused")
public interface ChatLogRepository extends MongoRepository<ChatLog, String> {
}
