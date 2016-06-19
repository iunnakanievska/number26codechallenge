package eu.number26.codechallenge.rest.controller;

import eu.number26.codechallenge.dao.TransactionDao;
import eu.number26.codechallenge.model.Transaction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ikanievska
 */
@RestController
@RequestMapping("/transactionservice")
public class TransactionController {

    @Resource(name = "${datasource}")
    private TransactionDao transactionDao;

    private static Map<String, String> status(String message) {
        Map<String, String> status = new HashMap<>();
        status.put("status", message);
        return status;
    }

    @RequestMapping(value = "/transaction/{transactionId}", method = RequestMethod.PUT)
    public ResponseEntity putTransaction(@PathVariable("transactionId") long transactionId, @RequestBody Transaction transaction) {
        transaction.setTransactionId(transactionId);
        Transaction mergedTransaction = transactionDao.merge(transaction);
        if (mergedTransaction == null) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(status("Wrong parent transaction: " + transaction.getParentId()));
        }
        return ResponseEntity.ok(status("ok"));//mergedTransaction);
    }

    @RequestMapping(value = "/transaction/{transactionId}", method = RequestMethod.GET)
    public ResponseEntity getTransaction(@PathVariable("transactionId") long transactionId) {
        Transaction transaction = transactionDao.getById(transactionId);
        if (transaction == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(status("Transaction not found: " + transactionId));
        }
        return ResponseEntity.ok(transaction);
    }

    @RequestMapping(value = "/types/{type}", method = RequestMethod.GET)
    public ResponseEntity<Collection<Long>> getTransactionsByType(@PathVariable("type") String type) {
        return ResponseEntity.ok(transactionDao.transactionsByType(type)
                .stream()
                .map(Transaction::getTransactionId)
                .collect(Collectors.toList()));
    }

    @RequestMapping(value = "/sum/{transactionId}", method = RequestMethod.GET)
    public ResponseEntity getTransactionSum(@PathVariable("transactionId") Long transactionId) {
        Double transactionTotalAmount = transactionDao.transactionTotalAmount(transactionId);
        if (transactionTotalAmount == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(status("Transaction not found: " + transactionId));
        } else {
            return ResponseEntity.ok(new HashMap<String, Double>() {{
                this.put("sum", transactionTotalAmount);
            }});
        }
    }

    @RequestMapping(value = "/drop", method = RequestMethod.GET)
    public void drop() {
        transactionDao.clear();
    }
}
