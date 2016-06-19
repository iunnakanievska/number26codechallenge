package eu.number26.codechallenge.model;

import org.junit.Test;

import static eu.number26.codechallenge.TestHelper.createTransaction;
import static org.junit.Assert.assertEquals;

/**
 * @author ikanievska
 */
public class TransactionTest {
    @Test
    public void singleTransaction_TransactionTotalAmount_ShouldReturnTransactionAmount() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);
        assertEquals(Double.valueOf(parent.getAmount()), Double.valueOf(parent.getTotalAmount()));
    }

    @Test
    public void transactionWithChild_getTotalAmount_ShouldReturnTransactionAmountTransitively() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);
        Transaction firstChild = createTransaction(2L, parent.getTransactionId(), "shopping", 2745.7);
        Transaction secondChild = createTransaction(3L, parent.getTransactionId(), "book", 1000000.0);
        Transaction grandChild = createTransaction(4L, firstChild.getTransactionId(), "blazer", 77.99);
        firstChild.getChildTransactions().add(grandChild);
        parent.getChildTransactions().add(firstChild);
        parent.getChildTransactions().add(secondChild);
        Double expectedSum = parent.getAmount() + firstChild.getAmount() + secondChild.getAmount() + grandChild.getAmount();
        assertEquals(expectedSum, Double.valueOf(parent.getTotalAmount()));
    }
}