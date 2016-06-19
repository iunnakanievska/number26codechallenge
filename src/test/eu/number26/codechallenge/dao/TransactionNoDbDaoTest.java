package eu.number26.codechallenge.dao;

import eu.number26.codechallenge.model.Transaction;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static eu.number26.codechallenge.TestHelper.assertTransactionsEqual;
import static eu.number26.codechallenge.TestHelper.createTransaction;
import static org.junit.Assert.*;

/**
 * @author ikanievska
 */
public class TransactionNoDbDaoTest {
    private TransactionNoDbDao transactionDao;

    @Before
    public void setup() {
        this.transactionDao = new TransactionNoDbDao();
    }

    @Test
    public void newTransactionWithoutParent_Merge_ShouldStoreTransaction() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);
        Transaction actual = transactionDao.merge(parent);

        assertEquals(transactionDao.size(), 1);

        assertTransactionsEqual(parent, actual);
        assertEquals(parent.getChildTransactions().size(), actual.getChildTransactions().size());
    }

    @Test
    public void transactionWithoutParent_SetItselfAsParentAndMerge_ShouldReturnNullWithoutChanges() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);

        transactionDao.merge(parent);

        Transaction cycledParent = createTransaction(parent.getTransactionId(),
                parent.getTransactionId(),
                parent.getType(),
                parent.getAmount());

        Transaction updatedParent = transactionDao.merge(cycledParent);
        Transaction storedParent = transactionDao.getById(parent.getTransactionId());

        assertEquals(1, transactionDao.size());

        assertNull(updatedParent);
        assertTransactionsEqual(parent, storedParent);
        assertEquals(parent.getChildTransactions().size(), storedParent.getChildTransactions().size());
    }

    @Test
    public void existentTransactionWithoutParent_Merge_ShouldUpdateTransaction() throws Exception {
        Transaction transaction = createTransaction(1L, null, "car", 120000.75);
        transaction = transactionDao.merge(transaction);
        transaction.setAmount(transaction.getAmount() + 1);
        transaction.setType(transaction.getType().concat(" updated"));

        Transaction updatedTransaction = transactionDao.merge(transaction);

        assertEquals(1, transactionDao.size());
        assertTransactionsEqual(transaction, updatedTransaction);
        assertEquals(transaction.getChildTransactions().size(), updatedTransaction.getChildTransactions().size());
    }

    @Test
    public void newTransactionWithParent_Merge_ShouldStoreTransactionAndAddItToParentChildren() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);

        transactionDao.merge(parent);

        Transaction child = createTransaction(2L, 1L, "shopping", 2700.0);

        Transaction actualChild = transactionDao.merge(child);
        Transaction actualParent = transactionDao.getById(child.getParentId());

        assertEquals(2, transactionDao.size());

        assertTransactionsEqual(child, actualChild);
        assertEquals(child.getChildTransactions().size(), actualChild.getChildTransactions().size());

        assertTransactionsEqual(parent, actualParent);
        assertEquals(1, actualParent.getChildTransactions().size());
        assertTrue(actualParent.getChildTransactions().contains(child));
    }

    @Test
    public void newTransactionWithInexistentParent_Merge_ShouldReturnNullWithoutStorageChanges() throws Exception {
        Transaction child = createTransaction(2L, 1L, "shopping", 2700.0);

        Transaction actualChild = transactionDao.merge(child);

        assertEquals(0, transactionDao.size());
        assertNull(actualChild);
    }

    @Test
    public void existentTransactionWithoutParent_AddParentAndMerge_ShouldUpdateTransactionAndAddItToParentChildren() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);

        transactionDao.merge(parent);

        Transaction child = createTransaction(2L, null, "shopping", 2700.0);

        transactionDao.merge(child);

        Transaction childUpdate = createTransaction(child.getTransactionId(),
                parent.getTransactionId(),
                child.getType().concat(" updated"),
                child.getAmount() + 1);

        Transaction updatedChild = transactionDao.merge(childUpdate);
        Transaction updatedParent = transactionDao.getById(childUpdate.getParentId());

        assertEquals(2, transactionDao.size());

        assertTransactionsEqual(childUpdate, updatedChild);
        assertEquals(childUpdate.getChildTransactions().size(), updatedChild.getChildTransactions().size());

        assertTransactionsEqual(parent, updatedParent);
        assertEquals(1, updatedParent.getChildTransactions().size());
        assertTrue(updatedParent.getChildTransactions().contains(child));
    }

    @Test
    public void parentWithOneChild_RemoveParentAndMerge_ShouldUpdateTransactionAndRemoveItFromParentChildren() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);

        transactionDao.merge(parent);

        Transaction child = createTransaction(2L, parent.getTransactionId(), "shopping", 2700.0);

        transactionDao.merge(child);

        Transaction childUpdate = createTransaction(child.getTransactionId(),
                null,
                child.getType().concat(" updated"),
                child.getAmount() + 1);

        Transaction updatedChild = transactionDao.merge(childUpdate);
        Transaction updatedParent = transactionDao.getById(parent.getTransactionId());

        assertEquals(2, transactionDao.size());

        assertTransactionsEqual(childUpdate, updatedChild);
        assertEquals(childUpdate.getChildTransactions().size(), updatedChild.getChildTransactions().size());

        assertTransactionsEqual(parent, updatedParent);
        assertTrue(updatedParent.getChildTransactions().isEmpty());
    }

    @Test
    public void transactionWithChildren_RemoveParentAndMerge_ShouldRemoveSingleChild() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);

        transactionDao.merge(parent);

        Transaction firstChild = createTransaction(2L, parent.getTransactionId(), "shopping", 2700.0);
        Transaction secondChild = createTransaction(3L, parent.getTransactionId(), "shopping", 2700.0);

        transactionDao.merge(firstChild);
        transactionDao.merge(secondChild);

        Transaction firstChildUpdate = createTransaction(firstChild.getTransactionId(),
                null,
                firstChild.getType(),
                firstChild.getAmount());

        Transaction updatedChild = transactionDao.merge(firstChildUpdate);
        Transaction updatedParent = transactionDao.getById(parent.getTransactionId());

        assertEquals(3, transactionDao.size());

        assertTransactionsEqual(firstChildUpdate, updatedChild);
        assertEquals(firstChildUpdate.getChildTransactions().size(), updatedChild.getChildTransactions().size());

        assertTransactionsEqual(parent, updatedParent);
        assertEquals(1, updatedParent.getChildTransactions().size());
        assertFalse(updatedParent.getChildTransactions().contains(firstChild));
        assertTrue(updatedParent.getChildTransactions().contains(secondChild));
    }

    @Test
    public void transactionWithChild_AddAnotherChildAndMerge_ShouldAddNewChildWithoutAffectionOfExistent() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);

        transactionDao.merge(parent);

        Transaction firstChild = createTransaction(2L, null, "shopping", 2700.0);
        Transaction secondChild = createTransaction(3L, parent.getTransactionId(), "shopping", 2700.0);

        transactionDao.merge(firstChild);
        transactionDao.merge(secondChild);

        Transaction firstChildUpdate = createTransaction(firstChild.getTransactionId(),
                parent.getTransactionId(),
                firstChild.getType(),
                firstChild.getAmount());

        Transaction updatedChild = transactionDao.merge(firstChildUpdate);
        Transaction updatedParent = transactionDao.getById(parent.getTransactionId());

        assertEquals(3, transactionDao.size());

        assertTransactionsEqual(firstChildUpdate, updatedChild);
        assertEquals(firstChildUpdate.getChildTransactions().size(), updatedChild.getChildTransactions().size());

        assertTransactionsEqual(parent, updatedParent);
        assertEquals(2, updatedParent.getChildTransactions().size());
        assertTrue(updatedParent.getChildTransactions().contains(firstChild));
        assertTrue(updatedParent.getChildTransactions().contains(secondChild));
    }

    @Test
    public void transactionWithChildren_ChangeParentAndMerge_ShouldUpdateParents() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);

        transactionDao.merge(parent);

        Transaction firstChild = createTransaction(2L, parent.getTransactionId(), "shopping", 2700.0);
        Transaction secondChild = createTransaction(3L, parent.getTransactionId(), "shopping", 2700.0);

        transactionDao.merge(firstChild);
        transactionDao.merge(secondChild);

        Transaction firstChildUpdate = createTransaction(firstChild.getTransactionId(),
                secondChild.getTransactionId(),
                firstChild.getType(),
                firstChild.getAmount());

        Transaction updatedFirstChild = transactionDao.merge(firstChildUpdate);
        Transaction updatedParent = transactionDao.getById(parent.getTransactionId());
        Transaction updatedSecondChild = transactionDao.getById(secondChild.getTransactionId());

        assertEquals(3, transactionDao.size());

        assertTransactionsEqual(firstChildUpdate, updatedFirstChild);
        assertEquals(firstChildUpdate.getChildTransactions().size(), updatedFirstChild.getChildTransactions().size());

        assertTransactionsEqual(parent, updatedParent);
        assertEquals(1, updatedParent.getChildTransactions().size());
        assertFalse(updatedParent.getChildTransactions().contains(firstChild));
        assertTrue(updatedParent.getChildTransactions().contains(secondChild));

        assertTransactionsEqual(secondChild, updatedSecondChild);
        assertEquals(1, updatedSecondChild.getChildTransactions().size());
        assertTrue(updatedSecondChild.getChildTransactions().contains(updatedFirstChild));
    }

    @Test
    public void twoTransactions_SetParentToEachOtherAndMerge_ShouldReturnNullWithoutStorageChanges() throws Exception {
        Transaction firstTransaction = createTransaction(2L, null, "shopping", 2700.0);
        Transaction secondTransaction = createTransaction(3L, firstTransaction.getTransactionId(), "shopping", 2700.0);

        transactionDao.merge(firstTransaction);
        transactionDao.merge(secondTransaction);

        Transaction firstTransactionUpdate = createTransaction(firstTransaction.getTransactionId(),
                secondTransaction.getTransactionId(),
                firstTransaction.getType(),
                firstTransaction.getAmount());

        Transaction updatedFirstTransaction = transactionDao.merge(firstTransactionUpdate);
        Transaction storedFirstTransaction = transactionDao.getById(firstTransaction.getTransactionId());
        Transaction storedSecondTransaction = transactionDao.getById(secondTransaction.getTransactionId());

        assertEquals(2, transactionDao.size());

        assertNull(updatedFirstTransaction);

        assertEquals(firstTransaction.getTransactionId(), storedFirstTransaction.getTransactionId());
        assertEquals(firstTransaction.getParentId(), storedFirstTransaction.getParentId());
        assertEquals(firstTransaction.getType(), storedFirstTransaction.getType());
        assertEquals(Double.valueOf(firstTransaction.getAmount()), Double.valueOf(storedFirstTransaction.getAmount()));
        assertEquals(1, storedFirstTransaction.getChildTransactions().size());
        assertTrue(storedFirstTransaction.getChildTransactions().contains(storedSecondTransaction));

        assertEquals(secondTransaction.getTransactionId(), storedSecondTransaction.getTransactionId());
        assertEquals(secondTransaction.getType(), storedSecondTransaction.getType());
        assertEquals(Double.valueOf(secondTransaction.getAmount()), Double.valueOf(storedSecondTransaction.getAmount()));
        assertNotNull(storedSecondTransaction.getChildTransactions());
        assertEquals(0, storedSecondTransaction.getChildTransactions().size());
    }

    @Test
    public void transactionWithChild_UpdateParentAndMerge_ShouldUpdateParentdWithoutAffectionOfChildren() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);

        transactionDao.merge(parent);

        Transaction child = createTransaction(2L, parent.getTransactionId(), "shopping", 2700.0);

        transactionDao.merge(child);

        Transaction parentUpdate = createTransaction(parent.getTransactionId(),
                parent.getParentId(),
                parent.getType(),
                parent.getAmount());

        Transaction updatedParent = transactionDao.merge(parentUpdate);
        Transaction foundChild = transactionDao.getById(child.getTransactionId());

        assertEquals(2, transactionDao.size());

        assertTransactionsEqual(parentUpdate, updatedParent);
        assertEquals(1, updatedParent.getChildTransactions().size());
        assertTrue(updatedParent.getChildTransactions().contains(foundChild));

        assertTransactionsEqual(child, foundChild);
        assertEquals(child.getChildTransactions().size(), foundChild.getChildTransactions().size());
    }

    @Test
    public void inexistentTransactionId_GetById_ShouldReturnNull() {
        Transaction inexistentTransaction = transactionDao.getById(0L);

        assertNull(inexistentTransaction);
    }

    @Test
    public void existentTransactionId_GetById_ShouldReturnTransaction() {
        Transaction transaction = createTransaction(1L, null, "car", 120000.75);

        transactionDao.merge(transaction);

        Transaction foundTransaction = transactionDao.getById(transaction.getTransactionId());

        assertNotNull(foundTransaction);
        assertTransactionsEqual(transaction, foundTransaction);
    }

    @Test
    public void existentTransactions_TransactionsByType_ShouldReturnSpecifiedTransactions() throws Exception {
        String carType = "car";
        String shoppingType = "shopping";
        List<Transaction> carTransactions = new LinkedList<>();
        List<Transaction> shoppingTransactions = new LinkedList<>();
        for (long i = 0; i < 10; ++i) {
            Transaction transaction = createTransaction(i, null, carType, 127000.0);
            transactionDao.merge(transaction);
            carTransactions.add(transaction);

        }
        for (long i = 0; i < 25; ++i) {
            Transaction transaction = createTransaction(i + carTransactions.size(), null, shoppingType, 2700.0);
            transactionDao.merge(transaction);
            shoppingTransactions.add(transaction);
        }

        Collection<Transaction> foundCarTransactions = transactionDao.transactionsByType(carType);
        Collection<Transaction> foundShoppingTransactions = transactionDao.transactionsByType(shoppingType);

        assertNotNull(foundCarTransactions);
        assertEquals(carTransactions.size(), foundCarTransactions.size());
        for (Transaction carTransaction : carTransactions) {
            assertTrue(foundCarTransactions.contains(carTransaction));
        }

        assertNotNull(foundShoppingTransactions);
        assertEquals(shoppingTransactions.size(), foundShoppingTransactions.size());
        for (Transaction shoppingTransaction : shoppingTransactions) {
            assertTrue(foundShoppingTransactions.contains(shoppingTransaction));
        }
    }

    @Test
    public void noTransactions_TransactionsByType_ShouldReturnEmptyCollection() {
        Collection<Transaction> foundTransactions = transactionDao.transactionsByType("inexistent type");
        assertNotNull(foundTransactions);
        assertTrue(foundTransactions.isEmpty());
    }

    @Test
    public void nullAsType_TransactionsByType_ShouldReturnEmptyCollection() {
        Collection<Transaction> foundTransactions = transactionDao.transactionsByType(null);
        assertNotNull(foundTransactions);
        assertTrue(foundTransactions.isEmpty());
    }

    @Test
    public void inexistentTransaction_TransactionTotalAmount_ShouldReturnNull() throws Exception {
        assertNull(transactionDao.transactionTotalAmount(0L));
    }

    @Test
    public void singleTransaction_TransactionTotalAmount_ShouldReturnTransactionAmount() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);
        transactionDao.merge(parent);
        assertEquals(Double.valueOf(parent.getAmount()), transactionDao.transactionTotalAmount(parent.getTransactionId()));
    }

    @Test
    public void transactionWithChild_TransactionTotalAmount_ShouldReturnTransactionAmountTransitively() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);
        Transaction firstChild = createTransaction(2L, parent.getTransactionId(), "shopping", 2745.7);
        Transaction secondChild = createTransaction(3L, parent.getTransactionId(), "book", 1000000.0);
        Transaction grandChild = createTransaction(4L, firstChild.getTransactionId(), "blazer", 77.99);
        transactionDao.merge(parent);
        transactionDao.merge(firstChild);
        transactionDao.merge(secondChild);
        transactionDao.merge(grandChild);
        Double expectedSum = parent.getAmount() + firstChild.getAmount() + secondChild.getAmount() + grandChild.getAmount();
        assertEquals(expectedSum, transactionDao.transactionTotalAmount(parent.getTransactionId()));
    }

    @Test
    public void noTransactions_Size_ShouldReturnZero() {
        assertEquals(0, transactionDao.size());
    }

    @Test
    public void noTransactions_AddTransactionsAndSize_ShouldReturnTransactionsNumber() {
        int transactionsNumber = 42;
        for (long i = 0; i < transactionsNumber; ++i) {
            Transaction transaction = createTransaction(i, null, null, null);
            transactionDao.merge(transaction);
        }
        assertEquals(transactionsNumber, transactionDao.size());
    }

    @Test
    public void transactionExists_UpdateTransactionAndSize_SizeShouldNotBeChanged() {
        Transaction transaction = createTransaction(0L, null, null, null);
        transactionDao.merge(transaction);
        long size = transactionDao.size();
        transactionDao.merge(transaction);
        assertEquals(size, transactionDao.size());
    }

    @Test
    public void noTransactions_Clear_ShouldWorkWithoutExceptions() {
        transactionDao.clear();
    }

    @Test
    public void transactionsExist_Clear_ShouldDropAllTransactions() {
        int transactionsNumber = 42;
        for (long i = 0; i < transactionsNumber; ++i) {
            Transaction transaction = createTransaction(i, null, null, null);
            transactionDao.merge(transaction);
        }
        transactionDao.clear();
        assertEquals(0, transactionDao.size());
    }
}