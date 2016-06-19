package eu.number26.codechallenge.rest.controller;

import eu.number26.codechallenge.dao.TransactionDao;
import eu.number26.codechallenge.model.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static eu.number26.codechallenge.TestHelper.assertTransactionsEqual;
import static eu.number26.codechallenge.TestHelper.createTransaction;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * @author ikanievska
 */
public class TransactionControllerTest {
    @InjectMocks
    private TransactionController transactionController;
    @Mock
    private TransactionDao transactionDao;
    private Map<String, String> expectedStatus = new HashMap<>();
    private Map<String, Double> expectedSum = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void transaction_getTransaction_ShouldReturnTransaction() {
        Transaction transaction = createTransaction(1L, null, "cars", 666.0);
        when(transactionDao.getById(transaction.getTransactionId())).thenReturn(transaction);

        ResponseEntity<Transaction> responseEntity = transactionController.getTransaction(transaction.getTransactionId());

        verify(transactionDao).getById(transaction.getTransactionId());
        verifyNoMoreInteractions(transactionDao);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTransactionsEqual(transaction, responseEntity.getBody());
    }

    @Test
    public void inexistentTransactionId_getTransaction_ShouldReturnNotFoundStatus() {
        when(transactionDao.getById(anyLong())).thenReturn(null);

        Long inexistentTransactionId = 0L;
        ResponseEntity<Map> responseEntity = transactionController.getTransaction(inexistentTransactionId);

        verify(transactionDao).getById(inexistentTransactionId);
        verifyNoMoreInteractions(transactionDao);

        assertNotNull(responseEntity);
        assertTrue(responseEntity.hasBody());
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        expectedStatus.put("status", "Transaction not found: " + inexistentTransactionId);
        assertEquals(expectedStatus, responseEntity.getBody());
    }

    @Test
    public void validTransaction_PutTransaction_ShouldReturnOk() {
        Transaction transaction = createTransaction(1L, null, "car", 666.0);
        when(transactionDao.merge(transaction)).thenReturn(transaction);

        ResponseEntity<Map> responseEntity = transactionController.putTransaction(transaction.getTransactionId(), transaction);

        verify(transactionDao).merge(transaction);
        verifyNoMoreInteractions(transactionDao);

        assertNotNull(responseEntity);
        assertTrue(responseEntity.hasBody());
        expectedStatus.put("status", "ok");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(expectedStatus, responseEntity.getBody());
    }

    @Test
    public void invalidTransaction_PutTransaction_ShouldReturnUnprocessableEntity() {
        Transaction transaction = createTransaction(1L, 2L, "car", 666.0);
        when(transactionDao.merge(any())).thenReturn(null);

        ResponseEntity<Map> responseEntity = transactionController.putTransaction(transaction.getTransactionId(), transaction);

        verify(transactionDao).merge(transaction);
        verifyNoMoreInteractions(transactionDao);

        assertNotNull(responseEntity);
        assertTrue(responseEntity.hasBody());
        expectedStatus.put("status", "Wrong parent transaction: " + transaction.getParentId());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntity.getStatusCode());
        assertEquals(expectedStatus, responseEntity.getBody());
    }

    @Test
    public void transactionsWithType_GetTransactionsByType_ShouldReturnTransactionsIdst() {
        String type = "car";
        Transaction transaction = createTransaction(1L, null, type, 666.0);
        Collection<Transaction> transactions = new LinkedList<>();
        transactions.add(transaction);

        when(transactionDao.transactionsByType(type)).thenReturn(transactions);

        ResponseEntity<Collection<Long>> responseEntity = transactionController.getTransactionsByType(type);

        verify(transactionDao).transactionsByType(type);
        verifyNoMoreInteractions(transactionDao);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.hasBody());
        assertEquals(transactions.size(), responseEntity.getBody().size());
        for (Transaction transacton : transactions) {
            assertTrue(responseEntity.getBody().contains(transaction.getTransactionId()));
        }
    }

    @Test
    public void noTransactionsWithType_GetTransactionsByType_ShouldReturnEmptyCollection() {
        String type = "type";

        when(transactionDao.transactionsByType(type)).thenReturn(Collections.emptyList());

        ResponseEntity<Collection<Long>> responseEntity = transactionController.getTransactionsByType(type);

        verify(transactionDao).transactionsByType(type);
        verifyNoMoreInteractions(transactionDao);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(responseEntity.hasBody());
        assertTrue(responseEntity.getBody().isEmpty());
    }

    @Test
    public void inexistentTransaction_GetTotalAmount_ShouldReturnNotFoundStatus() {
        Long inexistentTransactionId = 0L;

        when(transactionDao.transactionTotalAmount(anyLong())).thenReturn(null);

        ResponseEntity<Map> responseEntity = transactionController.getTransactionSum(inexistentTransactionId);

        verify(transactionDao).transactionTotalAmount(inexistentTransactionId);
        verifyNoMoreInteractions(transactionDao);

        assertNotNull(responseEntity);
        assertTrue(responseEntity.hasBody());
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        expectedStatus.put("status", "Transaction not found: " + inexistentTransactionId);
        assertEquals(expectedStatus, responseEntity.getBody());
    }

    @Test
    public void transaction_GetTotalAmount_ShouldReturnTotalAmount() {
        Long transactionId = 7L;
        Double sum = 777777777.7;

        when(transactionDao.transactionTotalAmount(transactionId)).thenReturn(sum);

        ResponseEntity<Map> responseEntity = transactionController.getTransactionSum(transactionId);

        verify(transactionDao).transactionTotalAmount(transactionId);
        verifyNoMoreInteractions(transactionDao);

        assertNotNull(responseEntity);
        assertTrue(responseEntity.hasBody());
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        expectedSum.put("sum", sum);
        assertEquals(expectedSum, responseEntity.getBody());
    }
}