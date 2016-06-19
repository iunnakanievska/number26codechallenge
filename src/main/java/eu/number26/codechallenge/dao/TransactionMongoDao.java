package eu.number26.codechallenge.dao;

import eu.number26.codechallenge.model.Transaction;
import eu.number26.codechallenge.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * DAO for Transactions Mongo Repository.
 * <p>
 * Handles transactions and cascade operations during merge, since Spring Data MongoDB doesn't provide such functionality.
 *
 * @author ikanievska
 */
@Component("transactionMongoDao")
public class TransactionMongoDao implements TransactionDao {
    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    public synchronized Transaction merge(Transaction transaction) {
        if (transaction.getTransactionId() != null
                && transaction.getTransactionId().equals(transaction.getParentId())) {
            return null;
        }
        Transaction currentTransaction = transactionRepository.findOne(transaction.getTransactionId());
        if (currentTransaction != null) {
            transaction.setChildTransactions(currentTransaction.getChildTransactions());
        }
        // Register in parent transaction
        if (transaction.getParentId() != null) {
            Transaction parentTransaction = transactionRepository.findOne(transaction.getParentId());
            if (parentTransaction == null || transaction.getChildTransactions().contains(parentTransaction)) {
                return null;
            } else if (currentTransaction == null
                    || !transaction.getParentId().equals(currentTransaction.getParentId())) {
                parentTransaction.getChildTransactions().add(transaction);
                transactionRepository.save(parentTransaction);
            }
        }
        // Unregister from current parent transaction if transaction already exists
        if (currentTransaction != null) {
            if (currentTransaction.getParentId() != null &&
                    !currentTransaction.getParentId().equals(transaction.getParentId())) {
                Transaction currentParrent = transactionRepository.findOne(currentTransaction.getParentId());
                currentParrent.getChildTransactions().remove(currentTransaction);
                transactionRepository.save(currentParrent);
            }
        }
        return transactionRepository.save(transaction);
    }

    @Override
    public Transaction getById(Long transactionId) {
        return transactionRepository.findOne(transactionId);
    }

    @Override
    public Collection<Transaction> transactionsByType(String type) {
        return transactionRepository.findByType(type);
    }

    @Override
    public Double transactionTotalAmount(Long transactionId) {
        Transaction transaction = transactionRepository.findOne(transactionId);
        if (transaction == null) {
            return null;
        }
        return transaction.getTotalAmount();
    }

    @Override
    public void clear() {
        transactionRepository.deleteAll();
    }

    public long size() {
        return transactionRepository.count();
    }
}
