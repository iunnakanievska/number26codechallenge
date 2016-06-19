package eu.number26.codechallenge.dao;

import eu.number26.codechallenge.model.Transaction;

import java.util.Collection;

/**
 * @author ikanievska
 */
public interface TransactionDao {
    /**
     * Save transaction or update if transaction already exists.
     *
     * @param transaction to merge
     * @return {@code null} if parentId is invalid (id of its child or inexistent transaction) else - merged transaction
     */
    Transaction merge(Transaction transaction);

    Transaction getById(Long transactionId);

    Collection<Transaction> transactionsByType(String type);

    /**
     * Calculate total amount of specified transaction and its children transitively
     *
     * @param transactionId id of the parent transaction
     * @return total amount for specified transaction
     */
    Double transactionTotalAmount(Long transactionId);

    void clear();
}
