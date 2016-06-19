package eu.number26.codechallenge.dao;

import eu.number26.codechallenge.model.Transaction;
import eu.number26.codechallenge.repository.TransactionRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static eu.number26.codechallenge.TestHelper.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author ikanievska
 */
public class TransactionMongoDaoTest {
    @Mock
    private TransactionRepository transactionRepository;
    @InjectMocks
    private TransactionMongoDao transactionDao;

    @Before
    public void setupMock() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void newTransactionWithoutParent_Merge_ShouldStoreTransaction() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);

        when(transactionRepository.findOne(parent.getParentId())).thenReturn(null);
        when(transactionRepository.findOne(parent.getTransactionId())).thenReturn(null);
        when(transactionRepository.save(parent)).thenReturn(parent);

        Transaction actual = transactionDao.merge(parent);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).findOne(parent.getTransactionId());
        verify(transactionRepository).save(transactionCaptor.capture());
        verifyNoMoreInteractions(transactionRepository);

        Transaction capturedTransaction = transactionCaptor.getValue();

        assertTransactionsEqual(parent, actual);
        assertEquals(parent.getChildTransactions().size(), actual.getChildTransactions().size());

        assertTransactionsEqual(parent, capturedTransaction);
        assertEquals(parent.getChildTransactions().size(), capturedTransaction.getChildTransactions().size());
    }

    @Test
    public void existentTransactionWithoutParent_Merge_ShouldUpdateTransaction() throws Exception {
        Transaction transaction = createTransaction(1L, null, "car", 120000.75);

        Transaction transactionUpdate = createTransaction(transaction.getTransactionId(),
                transaction.getParentId(),
                transaction.getType() + " updated",
                transaction.getAmount() + 1);

        when(transactionRepository.findOne(transaction.getTransactionId())).thenReturn(transaction);
        when(transactionRepository.save(transactionUpdate)).thenReturn(transactionUpdate);

        Transaction updatedTransaction = transactionDao.merge(transactionUpdate);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).findOne(transaction.getTransactionId());
        verify(transactionRepository).save(transactionCaptor.capture());
        verifyNoMoreInteractions(transactionRepository);

        Transaction capturedTransaction = transactionCaptor.getValue();

        assertTransactionsEqual(capturedTransaction, updatedTransaction);
        assertEquals(capturedTransaction.getChildTransactions().size(), updatedTransaction.getChildTransactions().size());

        assertTransactionsEqual(transactionUpdate, capturedTransaction);
        assertEquals(transactionUpdate.getChildTransactions().size(), capturedTransaction.getChildTransactions().size());
    }

    @Test
    public void newTransactionWithParent_Merge_ShouldStoreTransactionAndAddItToParentChildren() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);
        Transaction child = createTransaction(2L, 1L, "shopping", 2700.0);

        when(transactionRepository.findOne(parent.getTransactionId())).thenReturn(parent);
        when(transactionRepository.findOne(child.getTransactionId())).thenReturn(null);
        when(transactionRepository.save(child)).thenReturn(child);

        Transaction actualChild = transactionDao.merge(child);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).findOne(child.getTransactionId());
        verify(transactionRepository).findOne(parent.getTransactionId());
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        verifyNoMoreInteractions(transactionRepository);

        List<Transaction> capturedTransactions = transactionCaptor.getAllValues();
        assertNotNull(capturedTransactions);
        assertEquals(2, capturedTransactions.size());
        Transaction capturedParent = capturedTransactions.get(0);
        Transaction capturedChild = capturedTransactions.get(1);

        assertTransactionsEqual(child, actualChild);
        assertEquals(child.getChildTransactions().size(), actualChild.getChildTransactions().size());

        assertTransactionsEqual(child, capturedChild);
        assertEquals(child.getChildTransactions().size(), capturedChild.getChildTransactions().size());

        assertTransactionsEqual(parent, capturedParent);
        assertEquals(1, capturedParent.getChildTransactions().size());
        assertTrue(capturedParent.getChildTransactions().contains(child));
    }

    @Test
    public void newTransactionWithInexistentParent_Merge_ShouldReturnNullWithoutStorageChanges() throws Exception {
        Long inexistentParentId = 1L;
        Transaction child = createTransaction(2L, inexistentParentId, "shopping", 2700.0);

        when(transactionRepository.findOne(anyLong())).thenReturn(null);

        Transaction actualChild = transactionDao.merge(child);

        verify(transactionRepository).findOne(child.getTransactionId());
        verify(transactionRepository).findOne(child.getParentId());
        verifyNoMoreInteractions(transactionRepository);

        assertNull(actualChild);
    }

    @Test
    public void existentTransactionWithoutParent_AddParentAndMerge_ShouldUpdateTransactionAndAddItToParentChildren() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);

        when(transactionRepository.findOne(parent.getTransactionId())).thenReturn(parent);

        Transaction child = createTransaction(2L, null, "shopping", 2700.0);

        when(transactionRepository.findOne(child.getTransactionId())).thenReturn(child);

        Transaction childUpdate = createTransaction(child.getTransactionId(),
                parent.getTransactionId(),
                child.getType().concat(" updated"),
                child.getAmount() + 1);

        when(transactionRepository.save(childUpdate)).thenReturn(childUpdate);

        Transaction updatedChild = transactionDao.merge(childUpdate);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).findOne(child.getTransactionId());
        verify(transactionRepository).findOne(childUpdate.getParentId());
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        verifyNoMoreInteractions(transactionRepository);

        List<Transaction> capturedTransactions = transactionCaptor.getAllValues();
        assertNotNull(capturedTransactions);
        assertEquals(2, capturedTransactions.size());
        Transaction capturedParent = capturedTransactions.get(0);
        Transaction captredChild = capturedTransactions.get(1);

        assertTransactionsEqual(childUpdate, updatedChild);
        assertEquals(childUpdate.getChildTransactions().size(), updatedChild.getChildTransactions().size());

        assertTransactionsEqual(childUpdate, captredChild);
        assertEquals(childUpdate.getChildTransactions().size(), captredChild.getChildTransactions().size());

        assertTransactionsEqual(parent, capturedParent);
        assertEquals(1, capturedParent.getChildTransactions().size());
        assertTrue(capturedParent.getChildTransactions().contains(child));
    }

    @Test
    public void parentWithOneChild_RemoveParentAndMerge_ShouldUpdateTransactionAndRemoveItFromParentChildren() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);

        when(transactionRepository.findOne(parent.getTransactionId())).thenReturn(parent);

        Transaction child = createTransaction(2L, parent.getTransactionId(), "shopping", 2700.0);

        when(transactionRepository.findOne(child.getTransactionId())).thenReturn(child);

        Transaction childUpdate = createTransaction(child.getTransactionId(),
                null,
                child.getType().concat(" updated"),
                child.getAmount() + 1);

        when(transactionRepository.save(childUpdate)).thenReturn(childUpdate);

        Transaction updatedChild = transactionDao.merge(childUpdate);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).findOne(child.getTransactionId());
        verify(transactionRepository).findOne(parent.getTransactionId());
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        verifyNoMoreInteractions(transactionRepository);

        List<Transaction> capturedTransactions = transactionCaptor.getAllValues();
        assertNotNull(capturedTransactions);
        assertEquals(2, capturedTransactions.size());
        Transaction capturedParent = capturedTransactions.get(0);
        Transaction capturedChild = capturedTransactions.get(1);

        assertTransactionsEqual(childUpdate, updatedChild);
        assertEquals(childUpdate.getChildTransactions().size(), updatedChild.getChildTransactions().size());

        assertTransactionsEqual(childUpdate, capturedChild);
        assertEquals(childUpdate.getChildTransactions().size(), capturedChild.getChildTransactions().size());

        assertTransactionsEqual(parent, capturedParent);
        assertTrue(capturedParent.getChildTransactions().isEmpty());
    }

    @Test
    public void transactionWithChildren_RemoveParentAndMerge_ShouldRemoveSingleChild() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);
        Transaction firstChild = createTransaction(2L, parent.getTransactionId(), "shopping", 2700.0);
        Transaction secondChild = createTransaction(3L, parent.getTransactionId(), "shopping", 2700.0);
        parent.getChildTransactions().add(firstChild);
        parent.getChildTransactions().add(secondChild);

        when(transactionRepository.findOne(parent.getTransactionId())).thenReturn(parent);

        when(transactionRepository.findOne(firstChild.getTransactionId())).thenReturn(firstChild);
        when(transactionRepository.findOne(secondChild.getTransactionId())).thenReturn(secondChild);

        Transaction firstChildUpdate = createTransaction(firstChild.getTransactionId(),
                null,
                firstChild.getType(),
                firstChild.getAmount());

        when(transactionRepository.save(firstChildUpdate)).thenReturn(firstChildUpdate);

        Transaction updatedChild = transactionDao.merge(firstChildUpdate);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).findOne(firstChild.getTransactionId());
        verify(transactionRepository).findOne(parent.getTransactionId());
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        verifyNoMoreInteractions(transactionRepository);

        List<Transaction> capturedTransactions = transactionCaptor.getAllValues();
        assertNotNull(capturedTransactions);
        assertEquals(2, capturedTransactions.size());
        Transaction capturedParent = capturedTransactions.get(0);
        Transaction capturedChild = capturedTransactions.get(1);

        assertTransactionsEqual(firstChildUpdate, updatedChild);
        assertEquals(firstChildUpdate.getChildTransactions().size(), updatedChild.getChildTransactions().size());

        assertTransactionsEqual(firstChildUpdate, capturedChild);
        assertEquals(firstChildUpdate.getChildTransactions().size(), updatedChild.getChildTransactions().size());

        assertTransactionsEqual(parent, capturedParent);
        assertEquals(1, capturedParent.getChildTransactions().size());
        assertFalse(capturedParent.getChildTransactions().contains(firstChild));
        assertTrue(capturedParent.getChildTransactions().contains(secondChild));
    }

    @Test
    public void transactionWithChild_AddAnotherChildAndMerge_ShouldAddNewChildWithoutAffectionOfExistent() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);

        Transaction firstChild = createTransaction(2L, null, "shopping", 2700.0);
        Transaction secondChild = createTransaction(3L, parent.getTransactionId(), "shopping", 2700.0);
        parent.getChildTransactions().add(secondChild);

        when(transactionRepository.findOne(parent.getTransactionId())).thenReturn(parent);
        when(transactionRepository.findOne(firstChild.getTransactionId())).thenReturn(firstChild);
        when(transactionRepository.findOne(secondChild.getTransactionId())).thenReturn(secondChild);

        Transaction firstChildUpdate = createTransaction(firstChild.getTransactionId(),
                parent.getTransactionId(),
                firstChild.getType(),
                firstChild.getAmount());

        when(transactionRepository.save(firstChildUpdate)).thenReturn(firstChildUpdate);

        Transaction updatedChild = transactionDao.merge(firstChildUpdate);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).findOne(firstChild.getTransactionId());
        verify(transactionRepository).findOne(parent.getTransactionId());
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        verifyNoMoreInteractions(transactionRepository);

        List<Transaction> capturedTransactions = transactionCaptor.getAllValues();
        assertNotNull(capturedTransactions);
        assertEquals(2, capturedTransactions.size());
        Transaction capturedParent = capturedTransactions.get(0);
        Transaction capturedChild = capturedTransactions.get(1);

        assertTransactionsEqual(firstChildUpdate, updatedChild);
        assertEquals(firstChildUpdate.getChildTransactions().size(), updatedChild.getChildTransactions().size());

        assertTransactionsEqual(firstChildUpdate, capturedChild);
        assertEquals(firstChildUpdate.getChildTransactions().size(), capturedChild.getChildTransactions().size());

        assertTransactionsEqual(parent, capturedParent);
        assertEquals(2, capturedParent.getChildTransactions().size());
        assertTrue(capturedParent.getChildTransactions().contains(firstChild));
        assertTrue(capturedParent.getChildTransactions().contains(secondChild));
    }

    @Test
    public void transactionWithChildren_ChangeParentAndMerge_ShouldUpdateParents() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);
        Transaction firstChild = createTransaction(2L, parent.getTransactionId(), "shopping", 2700.0);
        Transaction secondChild = createTransaction(3L, parent.getTransactionId(), "shopping", 2700.0);
        parent.getChildTransactions().add(firstChild);
        parent.getChildTransactions().add(secondChild);

        when(transactionRepository.findOne(parent.getTransactionId())).thenReturn(parent);
        when(transactionRepository.findOne(firstChild.getTransactionId())).thenReturn(firstChild);
        when(transactionRepository.findOne(secondChild.getTransactionId())).thenReturn(secondChild);

        Transaction firstChildUpdate = createTransaction(firstChild.getTransactionId(),
                secondChild.getTransactionId(),
                firstChild.getType(),
                firstChild.getAmount());

        when(transactionRepository.save(firstChildUpdate)).thenReturn(firstChildUpdate);

        Transaction updatedFirstChild = transactionDao.merge(firstChildUpdate);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).findOne(firstChild.getTransactionId());
        verify(transactionRepository).findOne(parent.getTransactionId());
        verify(transactionRepository).findOne(secondChild.getTransactionId());
        verify(transactionRepository, times(3)).save(transactionCaptor.capture());
        verifyNoMoreInteractions(transactionRepository);

        List<Transaction> capturedTransactions = transactionCaptor.getAllValues();
        assertNotNull(capturedTransactions);
        assertEquals(3, capturedTransactions.size());
        Transaction capturedSecondChild = capturedTransactions.get(0);
        Transaction capturedParent = capturedTransactions.get(1);
        Transaction capturedFirstChild = capturedTransactions.get(2);

        assertTransactionsEqual(firstChildUpdate, updatedFirstChild);
        assertEquals(firstChildUpdate.getChildTransactions().size(), updatedFirstChild.getChildTransactions().size());

        assertTransactionsEqual(firstChildUpdate, capturedFirstChild);
        assertEquals(firstChildUpdate.getChildTransactions().size(), capturedFirstChild.getChildTransactions().size());

        assertTransactionsEqual(parent, capturedParent);
        assertEquals(1, capturedParent.getChildTransactions().size());
        assertFalse(capturedParent.getChildTransactions().contains(firstChild));
        assertTrue(capturedParent.getChildTransactions().contains(secondChild));

        assertTransactionsEqual(secondChild, capturedSecondChild);
        assertEquals(1, capturedSecondChild.getChildTransactions().size());
        assertTrue(capturedSecondChild.getChildTransactions().contains(updatedFirstChild));
    }

    @Test
    public void twoTransactions_SetParentToEachOtherAndMerge_ShouldReturnNullWithoutStorageChanges() throws Exception {
        Transaction firstTransaction = createTransaction(0L, null, "shopping", 2700.0);
        Transaction secondTransaction = createTransaction(1L, firstTransaction.getTransactionId(), "shopping", 2700.0);
        firstTransaction.getChildTransactions().add(secondTransaction);

        when(transactionRepository.findOne(firstTransaction.getTransactionId())).thenReturn(firstTransaction);
        when(transactionRepository.findOne(secondTransaction.getTransactionId())).thenReturn(secondTransaction);

        Transaction firstTransactionUpdate = createTransaction(firstTransaction.getTransactionId(),
                secondTransaction.getTransactionId(),
                firstTransaction.getType(),
                firstTransaction.getAmount());

        Transaction updatedFirstTransaction = transactionDao.merge(firstTransactionUpdate);

        assertNull(updatedFirstTransaction);

        verify(transactionRepository).findOne(firstTransaction.getTransactionId());
        verify(transactionRepository).findOne(secondTransaction.getTransactionId());
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    public void transactionWithChild_UpdateParentAndMerge_ShouldUpdateParentdWithoutAffectionOfChildren() throws Exception {
        Transaction parent = createTransaction(1L, null, "car", 120000.75);
        Transaction child = spy(createTransaction(2L, parent.getTransactionId(), "shopping", 2700.0));
        parent.getChildTransactions().add(child);

        when(transactionRepository.findOne(parent.getTransactionId())).thenReturn(parent);
        when(transactionRepository.findOne(child.getTransactionId())).thenReturn(child);

        Transaction parentUpdate = createTransaction(parent.getTransactionId(),
                parent.getParentId(),
                parent.getType(),
                parent.getAmount());

        when(transactionRepository.save(parentUpdate)).thenReturn(parentUpdate);

        Transaction updatedParent = transactionDao.merge(parentUpdate);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).findOne(parent.getTransactionId());
        verify(transactionRepository).save(transactionCaptor.capture());
        verifyNoMoreInteractions(transactionRepository);

        Transaction capturedParent = transactionCaptor.getValue();

        assertTransactionsEqual(parentUpdate, updatedParent);
        assertEquals(1, updatedParent.getChildTransactions().size());
        assertTrue(updatedParent.getChildTransactions().contains(child));

        assertTransactionsEqual(parentUpdate, capturedParent);
        assertEquals(1, capturedParent.getChildTransactions().size());
        assertTrue(capturedParent.getChildTransactions().contains(child));

        verifyNoModifications(child);
    }

    @Test
    public void transactionId_GetById_ShouldInvokeTransactionSearching() {
        Long transactionId = 0L;
        transactionDao.getById(transactionId);
        verify(transactionRepository).findOne(transactionId);
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    public void existentTransactions_TransactionsByType_ShouldReturnSpecifiedTransactions() throws Exception {
        String carType = "car";
        transactionDao.transactionsByType(carType);
        verify(transactionRepository).findByType(carType);
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    public void inexistentTransaction_TransactionTotalAmount_ShouldReturnNull() throws Exception {
        assertNull(transactionDao.transactionTotalAmount(0L));
    }

    @Test
    public void transaction_TransactionTotalAmount_ShouldInvokeTransactionTotalAmount() throws Exception {
        Long transactionId = 0L;
        Transaction parent = spy(createTransaction(transactionId, null, "car", 120000.75));
        when(transactionRepository.findOne(transactionId)).thenReturn(parent);
        transactionDao.transactionTotalAmount(transactionId);
        verify(transactionRepository).findOne(transactionId);
        verifyNoMoreInteractions(transactionRepository);
        verify(parent).getTotalAmount();
        verifyNoMoreInteractions(parent);
    }

    @Test
    public void any_Size_ShouldInvokeCount() {
        transactionDao.size();
        verify(transactionRepository).count();
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    public void any_Clear_ShouldDeleteAll() {
        transactionDao.clear();
        verify(transactionRepository).deleteAll();
        verifyNoMoreInteractions(transactionRepository);
    }
}