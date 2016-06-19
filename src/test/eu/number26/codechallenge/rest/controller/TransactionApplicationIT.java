package eu.number26.codechallenge.rest.controller;

import eu.number26.codechallenge.Application;
import eu.number26.codechallenge.dao.TransactionDao;
import eu.number26.codechallenge.model.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static eu.number26.codechallenge.TestHelper.assertTransactionsEqual;
import static eu.number26.codechallenge.TestHelper.createTransaction;
import static org.junit.Assert.*;

/**
 * @author ikanievska
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
@WebIntegrationTest("server.port: 8888")
@TestPropertySource(locations = "classpath:test.properties")
public class TransactionApplicationIT {
    private static final int SERVER_PORT = 8888;
    private static final String SERVER_URL = "http://localhost:" + SERVER_PORT + "/transactionservice";

    TestRestTemplate transactionRestTemplate = new TestRestTemplate();

    @Resource(name = "${datasource}")
    private TransactionDao transactionDao;

    private static String url(String relativeUrl) {
        return SERVER_URL + relativeUrl;
    }

    @Before
    public void setUp() throws Exception {
        transactionDao.clear();
    }

    @After
    public void tearDown() throws Exception {
        transactionDao.clear();
    }

    @Test
    public void testPutTransaction() throws Exception {
    }

    @Test
    public void testGetTransactionMongo() throws Exception {
    }

    @Test
    public void testGetTransactionsByType() throws Exception {

    }

    @Test
    public void testGetTransactionSum() throws Exception {

    }

    private ResponseEntity getTransaction(Long transactionId, Class expectedResponseType) {
        return transactionRestTemplate.getForEntity(
                url("/transaction/{transactionId}"),
                expectedResponseType,
                transactionId);
    }

    private ResponseEntity getTransaction(Long transactionId) {
        return getTransaction(transactionId, Transaction.class);
    }

    private ResponseEntity<Map> putTransaction(Transaction transaction, Long transactionId) {
        return transactionRestTemplate.exchange(
                url("/transaction/{transactionId}"),
                HttpMethod.PUT,
                new HttpEntity<>(transaction),
                Map.class,
                transactionId);
    }

    private ResponseEntity<Map> getSum(Long transactionId) {
        return transactionRestTemplate.getForEntity(
                url("/sum/{transactionId}"),
                Map.class,
                transactionId
        );
    }

    @Test
    public void newTransactionWithoutParent_Merge_ShouldStoreTransaction() throws Exception {
        Transaction transaction = createTransaction(null, null, "car", 127000.0);
        Long transactionId = 0L;

        ResponseEntity<Map> putResponse = putTransaction(
                transaction,
                transactionId);
        assertEquals(HttpStatus.OK, putResponse.getStatusCode());
        assertTrue(putResponse.hasBody());
        assertEquals("ok", putResponse.getBody().get("status"));

        ResponseEntity<Transaction> response = getTransaction(transactionId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTransactionsEqual(transaction, response.getBody());
    }

    @Test
    public void existentTransactionWithoutParent_Merge_ShouldUpdateTransaction() throws Exception {
        long transactionId = 1L;

        Transaction transaction = createTransaction(null, null, "car", 120000.75);

        putTransaction(transaction, transactionId);

        Transaction transactionUpdate = createTransaction(transaction.getTransactionId(),
                transaction.getParentId(),
                transaction.getType() + " updated",
                transaction.getAmount() + 1);

        putTransaction(transactionUpdate, transactionId);

        ResponseEntity<Transaction> response = getTransaction(transactionId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTransactionsEqual(transactionUpdate, response.getBody());
    }

    @Test
    public void newTransactionWithParent_Merge_ShouldStoreTransactionAndAddItToParentChildren() throws Exception {
        long parentId = 2L;
        Transaction parent = createTransaction(null, null, "car", 120000.75);
        long childId = 3L;
        Transaction child = createTransaction(null, parentId, "shopping", 2700.0);

        putTransaction(parent, parentId);
        putTransaction(child, childId);

        ResponseEntity<Transaction> parentResponse = getTransaction(parentId);
        assertEquals(HttpStatus.OK, parentResponse.getStatusCode());
        ResponseEntity<Transaction> childResponse = getTransaction(childId);
        assertEquals(HttpStatus.OK, childResponse.getStatusCode());
        Transaction actualParent = parentResponse.getBody();
        Transaction actualChild = childResponse.getBody();

        assertTransactionsEqual(child, actualChild);

        assertTransactionsEqual(parent, actualParent);
    }

    @Test
    public void newTransactionWithInexistentParent_Merge_ShouldReturnNullWithoutStorageChanges() throws Exception {
        Long inexistentParentId = 4L;
        Long childId = 5L;
        Transaction child = createTransaction(null, inexistentParentId, "shopping", 2700.0);

        ResponseEntity<Map> putResponse = putTransaction(child, childId);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, putResponse.getStatusCode());
        assertTrue(putResponse.hasBody());
        assertFalse(putResponse.getBody().isEmpty());
        assertTrue(putResponse.getBody().containsKey("status"));

        ResponseEntity<Map> response = getTransaction(childId, Map.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.hasBody());
        assertFalse(response.getBody().isEmpty());
        assertTrue(response.getBody().containsKey("status"));
    }

    @Test
    public void existentTransactionWithoutParent_AddParentAndMerge_ShouldUpdateTransactionAndAddItToParentChildren() throws Exception {
        Transaction parent = createTransaction(null, null, "car", 120000.75);
        Transaction child = createTransaction(null, null, "shopping", 2700.0);

        Long parentId = 7L;
        Long childId = 8L;

        putTransaction(parent, parentId);
        putTransaction(child, childId);

        Transaction childUpdate = createTransaction(child.getTransactionId(),
                parentId,
                child.getType().concat(" updated"),
                child.getAmount() + 1);

        ResponseEntity<Map> putResponse = putTransaction(childUpdate, childId);

        assertEquals(HttpStatus.OK, putResponse.getStatusCode());
        assertTrue(putResponse.hasBody());
        assertEquals("ok", putResponse.getBody().get("status"));

        ResponseEntity<Transaction> parentResponse = getTransaction(parentId);
        assertEquals(HttpStatus.OK, parentResponse.getStatusCode());
        ResponseEntity<Transaction> childResponse = getTransaction(childId);
        assertEquals(HttpStatus.OK, childResponse.getStatusCode());
        Transaction actualParent = parentResponse.getBody();
        Transaction actualChild = childResponse.getBody();

        assertTransactionsEqual(childUpdate, actualChild);

        assertTransactionsEqual(parent, actualParent);
    }

    @Test
    public void parentWithOneChild_RemoveParentAndMerge_ShouldUpdateTransactionAndRemoveItFromParentChildren() throws Exception {
        Transaction parent = createTransaction(null, null, "car", 120000.75);
        Long parentId = 9L;

        putTransaction(parent, parentId);

        Transaction child = createTransaction(null, parent.getTransactionId(), "shopping", 2700.0);
        Long childId = 10L;

        putTransaction(child, childId);

        Transaction childUpdate = createTransaction(child.getTransactionId(),
                null,
                child.getType().concat(" updated"),
                child.getAmount() + 1);

        ResponseEntity<Map> putResponse = putTransaction(childUpdate, childId);

        assertEquals(HttpStatus.OK, putResponse.getStatusCode());
        assertTrue(putResponse.hasBody());
        assertEquals("ok", putResponse.getBody().get("status"));

        ResponseEntity<Transaction> parentResponse = getTransaction(parentId);
        assertEquals(HttpStatus.OK, parentResponse.getStatusCode());
        ResponseEntity<Transaction> childResponse = getTransaction(childId);
        assertEquals(HttpStatus.OK, childResponse.getStatusCode());
        Transaction actualParent = parentResponse.getBody();
        Transaction actualChild = childResponse.getBody();

        assertTransactionsEqual(childUpdate, actualChild);

        assertTransactionsEqual(parent, actualParent);
    }

    @Test
    public void transactionWithChildren_ChangeParentAndMerge_ShouldUpdateParents() throws Exception {
        Transaction parent = createTransaction(null, null, "car", 120000.75);
        Transaction firstChild = createTransaction(null, parent.getTransactionId(), "shopping", 2700.0);
        Transaction secondChild = createTransaction(null, parent.getTransactionId(), "shopping", 2700.0);

        Long parentId = 11L;
        Long firstChildId = 12L;
        Long secondChildId = 13L;

        putTransaction(parent, parentId);
        putTransaction(firstChild, firstChildId);
        putTransaction(secondChild, secondChildId);

        Transaction firstChildUpdate = createTransaction(firstChild.getTransactionId(),
                secondChildId,
                firstChild.getType(),
                firstChild.getAmount());

        ResponseEntity<Map> putResponse = putTransaction(firstChildUpdate, firstChildId);

        assertEquals(HttpStatus.OK, putResponse.getStatusCode());
        assertTrue(putResponse.hasBody());
        assertEquals("ok", putResponse.getBody().get("status"));

        ResponseEntity<Transaction> parentResponse = getTransaction(parentId);
        assertEquals(HttpStatus.OK, parentResponse.getStatusCode());
        ResponseEntity<Transaction> firstChildResponse = getTransaction(firstChildId);
        assertEquals(HttpStatus.OK, firstChildResponse.getStatusCode());
        ResponseEntity<Transaction> secondChildResponse = getTransaction(secondChildId);
        assertEquals(HttpStatus.OK, secondChildResponse.getStatusCode());
        Transaction actualParent = parentResponse.getBody();
        Transaction actualFirstChild = firstChildResponse.getBody();
        Transaction actualSecondChild = secondChildResponse.getBody();

        assertTransactionsEqual(firstChildUpdate, actualFirstChild);

        assertTransactionsEqual(secondChild, actualSecondChild);

        assertTransactionsEqual(parent, actualParent);
    }

    @Test
    public void twoTransactions_SetParentToEachOtherAndMerge_ShouldReturnNullWithoutStorageChanges() throws Exception {
        Transaction firstTransaction = createTransaction(null, null, "shopping", 2700.0);
        Long firstTransactionId = 14L;
        Transaction secondTransaction = createTransaction(null, firstTransactionId, "shopping", 2700.0);
        Long secondTransactionId = 15L;

        putTransaction(firstTransaction, firstTransactionId);
        putTransaction(secondTransaction, secondTransactionId);

        Transaction firstTransactionUpdate = createTransaction(firstTransaction.getTransactionId(),
                secondTransactionId,
                firstTransaction.getType(),
                firstTransaction.getAmount());

        ResponseEntity<Map> putResponse = putTransaction(firstTransactionUpdate, firstTransactionId);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, putResponse.getStatusCode());
        assertTrue(putResponse.hasBody());
        assertFalse(putResponse.getBody().isEmpty());
        assertTrue(putResponse.getBody().containsKey("status"));

        ResponseEntity<Transaction> firstTransactionResponse = getTransaction(firstTransactionId);
        assertEquals(HttpStatus.OK, firstTransactionResponse.getStatusCode());
        ResponseEntity<Transaction> secondTransactionResponse = getTransaction(secondTransactionId);
        assertEquals(HttpStatus.OK, secondTransactionResponse.getStatusCode());
        Transaction actualFirstTransaction = firstTransactionResponse.getBody();
        Transaction actualSecondTransaction = secondTransactionResponse.getBody();

        assertTransactionsEqual(firstTransaction, actualFirstTransaction);

        assertTransactionsEqual(secondTransaction, actualSecondTransaction);
    }

    @Test
    public void transactionWithChild_UpdateParentAndMerge_ShouldUpdateParentdWithoutAffectionOfChildren() throws Exception {
        Transaction parent = createTransaction(null, null, "car", 120000.75);
        Long parentId = 16L;
        Transaction child = createTransaction(null, parentId, "shopping", 2700.0);
        Long childId = 17L;

        putTransaction(parent, parentId);
        putTransaction(child, childId);

        Transaction parentUpdate = createTransaction(parent.getTransactionId(),
                parent.getParentId(),
                parent.getType() + " updated",
                parent.getAmount() + 777);

        ResponseEntity<Map> putResponse = putTransaction(parentUpdate, parentId);

        assertEquals(HttpStatus.OK, putResponse.getStatusCode());
        assertTrue(putResponse.hasBody());
        assertEquals("ok", putResponse.getBody().get("status"));

        ResponseEntity<Transaction> parentResponse = getTransaction(parentId);
        assertEquals(HttpStatus.OK, parentResponse.getStatusCode());
        ResponseEntity<Transaction> childResponse = getTransaction(childId);
        assertEquals(HttpStatus.OK, childResponse.getStatusCode());
        Transaction actualParent = parentResponse.getBody();
        Transaction actualChild = childResponse.getBody();

        assertTransactionsEqual(child, actualChild);

        assertTransactionsEqual(parentUpdate, actualParent);
    }

    @Test
    public void inexistentTransactionId_GetById_ShouldReturn() {
        Long transactionId = 18L;
        ResponseEntity<Map> response = getTransaction(transactionId, Map.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.hasBody());
        assertFalse(response.getBody().isEmpty());
        assertTrue(response.getBody().containsKey("status"));
    }

    @Test
    public void existentTransactions_TransactionsByType_ShouldReturnSpecifiedTransactions() throws Exception {
        String carType = "car";
        String shoppingType = "shopping";
        List<Transaction> carTransactions = new LinkedList<>();
        List<Transaction> shoppingTransactions = new LinkedList<>();
        for (long i = 0; i < 10; ++i) {
            Transaction transaction = createTransaction(i, null, carType, 127000.0);
            putTransaction(transaction, transaction.getTransactionId());
            carTransactions.add(transaction);

        }
        for (long i = 0; i < 25; ++i) {
            Transaction transaction = createTransaction(i + carTransactions.size(), null, shoppingType, 2700.0);
            putTransaction(transaction, transaction.getTransactionId());
            shoppingTransactions.add(transaction);
        }

        ResponseEntity<Collection> foundCarTransactionsResponse = transactionRestTemplate.getForEntity(
                url("/types/{type}"),
                Collection.class,
                carType
        );
        ResponseEntity<Collection> foundShoppingTransactionsResponse = transactionRestTemplate.getForEntity(
                url("/types/{type}"),
                Collection.class,
                shoppingType
        );

        assertNotNull(foundCarTransactionsResponse);
        assertTrue(foundCarTransactionsResponse.hasBody());

        assertNotNull(foundShoppingTransactionsResponse);
        assertTrue(foundShoppingTransactionsResponse.hasBody());

        Collection<Long> foundCarTransactions = foundCarTransactionsResponse.getBody();
        Collection<Long> foundShoppingTransactions = foundShoppingTransactionsResponse.getBody();

        assertEquals(carTransactions.size(), foundCarTransactions.size());
        for (Transaction carTransaction : carTransactions) {
            assertTrue(foundCarTransactions.contains(carTransaction.getTransactionId().intValue()));
        }

        assertEquals(shoppingTransactions.size(), foundShoppingTransactions.size());
        for (Transaction shoppingTransaction : shoppingTransactions) {
            assertTrue(foundShoppingTransactions.contains(shoppingTransaction.getTransactionId().intValue()));
        }
    }

    @Test
    public void noTransactionsWithType_TransactionsByType_ShouldReturnNull() throws Exception {
        String type = "type";

        ResponseEntity<Collection> typeResponse = transactionRestTemplate.getForEntity(
                url("/types/{type}"),
                Collection.class,
                type
        );


        assertNotNull(typeResponse);
        assertTrue(typeResponse.hasBody());
        assertTrue(typeResponse.getBody().isEmpty());
    }

    @Test
    public void inexistentTransaction_TransactionTotalAmount_ShouldReturnNull() throws Exception {
        Long inexistentTransactionId = 19L;
        ResponseEntity<Map> sumResponse = getSum(inexistentTransactionId);
        assertEquals(HttpStatus.NOT_FOUND, sumResponse.getStatusCode());
        assertTrue(sumResponse.hasBody());
        assertTrue(sumResponse.getBody().containsKey("status"));
    }

    @Test
    public void transactionWithChild_TransactionTotalAmount_ShouldReturnTransactionAmountTransitively() throws Exception {
        Transaction parent = createTransaction(20L, null, "car", 120000.75);
        Transaction firstChild = createTransaction(21L, parent.getTransactionId(), "shopping", 2745.7);
        Transaction secondChild = createTransaction(22L, parent.getTransactionId(), "book", 1000000.0);
        Transaction grandChild = createTransaction(23L, firstChild.getTransactionId(), "blazer", 77.99);

        Double expectedSum = parent.getAmount() + firstChild.getAmount() + secondChild.getAmount() + grandChild.getAmount();

        putTransaction(parent, parent.getTransactionId());
        putTransaction(firstChild, firstChild.getTransactionId());
        putTransaction(secondChild, secondChild.getTransactionId());
        putTransaction(grandChild, grandChild.getTransactionId());


        ResponseEntity<Map> sumResponse = getSum(parent.getTransactionId());
        assertEquals(HttpStatus.OK, sumResponse.getStatusCode());
        assertTrue(sumResponse.hasBody());
        assertTrue(sumResponse.getBody().containsKey("sum"));
        assertEquals(expectedSum, sumResponse.getBody().get("sum"));
    }
}