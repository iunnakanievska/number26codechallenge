package eu.number26.codechallenge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

/**
 * @author ikanievska
 */
@Document
public class Transaction {
    @Id
    @JsonIgnore
    private Long transactionId;
    private Long parentId;
    @Indexed
    private String type;
    private double amount;
    @DBRef
    @JsonIgnore
    private Set<Transaction> childTransactions;

    public Transaction() {
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount == null ? 0 : amount;
    }

    public Set<Transaction> getChildTransactions() {
        if (this.childTransactions == null) {
            this.childTransactions = new HashSet<>();
        }
        return this.childTransactions;
    }

    public void setChildTransactions(Set<Transaction> childTransactions) {
        this.childTransactions = childTransactions;
    }

    @JsonIgnore
    @Transient
    public double getTotalAmount() {
        if (childTransactions == null || childTransactions.isEmpty()) {
            return amount;
        } else {
            return childTransactions.stream()
                    .map(Transaction::getTotalAmount)
                    .reduce(this.amount, (sum, subAmount) -> sum + subAmount);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof Transaction)) {
            return false;
        }
        return this.getTransactionId().equals(((Transaction) other).getTransactionId());
    }

    @Override
    public int hashCode() {
        return this.transactionId.hashCode();
    }
}
