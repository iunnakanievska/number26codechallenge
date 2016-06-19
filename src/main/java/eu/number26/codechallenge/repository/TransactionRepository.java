package eu.number26.codechallenge.repository;

import eu.number26.codechallenge.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

/**
 * Transaction Mongo Repository
 *
 * @author ikanievska
 */
public interface TransactionRepository extends MongoRepository<Transaction, Long> {
    List<Transaction> findByType(String type);

    Collection<Transaction> findByParentId(Long parentId);
}
