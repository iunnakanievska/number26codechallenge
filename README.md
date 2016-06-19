# README #

We would like to have a RESTful web service that stores some transactions (in memory is fine)
and returns information about those transactions.
The transactions to be stored have a type and an amount. The service should support returning all
transactions of a type. Also, transactions can be linked to each other (using a "parent_id") and we
need to know the total amount involved for all transactions linked to a particular transaction.

For example:

In detail the api spec looks like the following:

**PUT /transactionservice/transaction/$transaction_id**

Body: 

 `{ "amount":double,"type":string,"parent_id":long } `
 
where:

 - **transaction_id** is a long specifying a new transaction
 
 - **amount** is a double specifying the amount
 
 - **type** is a string specifying a type of the transaction.
 
 - **parent_id** is an optional long that may specify the parent transaction of this transaction. 

**GET /transactionservice/transaction/$transaction_id **
Returns: 

`{ "amount":double,"type":string,"parent_id":long } `

**GET /transactionservice/types/$type **

Returns: `[ long, long, .... ]`
 
A json list of all transaction ids that share the same type $type.

**GET /transactionservice/sum/$transaction_id **

Returns : `{ "sum", double }`

A sum of all transactions that are transitively linked by their parent_id to $transaction_id.

**Some simple examples would be:**

- `PUT /transactionservice/transaction/10 ` => `{ "amount": 5000, "type":"cars" }` => `{ "status": "ok" } `

- `PUT /transactionservice/transaction/11` =>`{ "amount": 10000, "type": "shopping", "parent_id": 10 }` => `{ "status": "ok" } `

- `GET /transactionservice/types/cars` => `[10]`

- `GET /transactionservice/sum/10` => `{"sum":15000} `

- `GET /transactionservice/sum/11` => `{"sum":10000} 

### Stack Of Technology ###

* Java8
* Spring Boot 1.3.5
* Spring Data MongoDB 1.8.4
* EmbeddedMongo 1.5.0
* Maven 3.5.1 

### Project details: ###

Two different realisation of interface TransactionDao: have been implemented in project:
  
* *TransactionMongoDao*
* *TransactionNoDbDao*
Required implementaion may be choosen in *application.properties* file using *datasource* property.

Entities returned by *TransactionNoDbDao* implementation are mutable and changes reflect on storage (it is pretty bulk and difficult task to create own storage)