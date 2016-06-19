package eu.number26.codechallenge.dao;

import eu.number26.codechallenge.model.Transaction;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Provides access to in-memory storage.
 * <p>
 * NOTE: all changes on returned entities reflect on storage
 *
 * @author ikanievska
 */
@Component("transactionNoDbDao")
public class TransactionNoDbDao implements TransactionDao {
    private final Map<Long, Transaction> transactions = new ConcurrentHashMap<>();

    @Override
    public synchronized Transaction merge(Transaction transaction) {
        if (transaction.getTransactionId() != null
                && transaction.getTransactionId().equals(transaction.getParentId())) {
            return null;
        }
        Transaction currentTransaction = transactions.get(transaction.getTransactionId());
        if (currentTransaction != null) {
            transaction.setChildTransactions(currentTransaction.getChildTransactions());
        }
        if (transaction.getParentId() != null) {
            // Register in parent transaction
            Transaction parentTransaction = transactions.get(transaction.getParentId());
            if (parentTransaction == null
                    || transaction.getChildTransactions().contains(parentTransaction)) {
                return null;
            } else {
                if (currentTransaction == null
                        || !transaction.getParentId().equals(currentTransaction.getParentId())) {
                    parentTransaction.getChildTransactions().add(transaction);
                }
            }
        }
        if (currentTransaction != null) {
            if (currentTransaction.getParentId() != null
                    && !currentTransaction.getParentId().equals(transaction.getParentId())) {
                // Unregister from current parent transaction if transaction already exists
                Transaction currentParent = transactions.get(currentTransaction.getParentId());
                currentParent.getChildTransactions().remove(currentTransaction);
            }
        }
        transactions.put(transaction.getTransactionId(), transaction);
        return transactions.get(transaction.getTransactionId());
    }

    @Override
    public Transaction getById(Long transactionId) {
        return transactions.get(transactionId);
    }

    @Override
    public Collection<Transaction> transactionsByType(String type) {
        return transactions.values().stream().filter(transaction -> type.equals(transaction.getType())).collect(Collectors.toList());
    }

    @Override
    public Double transactionTotalAmount(Long transactionId) {
        Transaction transaction = transactions.get(transactionId);
        if (transaction == null) {
            return null;
        }
        return transaction.getTotalAmount();
    }

    @Override
    public void clear() {
        transactions.clear();
    }

    public long size() {
        return transactions.size();
    }
}
