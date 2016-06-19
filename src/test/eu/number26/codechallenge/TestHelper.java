package eu.number26.codechallenge;

import eu.number26.codechallenge.model.Transaction;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author ikanievska
 */
public class TestHelper {
    public static Transaction createTransaction(Long transactionId,
                                                Long parentId,
                                                String type,
                                                Double amount) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setParentId(parentId);
        transaction.setType(type);
        transaction.setAmount(amount);
        return transaction;
    }

    public static void assertTransactionsEqual(Transaction expected, Transaction actual) {
        assertEquals(expected.getTransactionId(), actual.getTransactionId());
        assertEquals(expected.getParentId(), actual.getParentId());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(Double.valueOf(expected.getAmount()), Double.valueOf(actual.getAmount()));
    }

    public static void verifyNoModifications(Transaction transaction) {
        verify(transaction, never()).setTransactionId(any());
        verify(transaction, never()).setParentId(any());
        verify(transaction, never()).setType(any());
        verify(transaction, never()).setAmount(any());
        verify(transaction, never()).setChildTransactions(any());
        verify(transaction, never()).getChildTransactions();
    }
}
