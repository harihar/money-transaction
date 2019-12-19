package com.revolut.controller

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Provides
import com.revolut.exceptions.InsufficientBalanceException
import com.revolut.exceptions.InvalidAccountException
import com.revolut.model.TransactionRequest
import com.revolut.services.TransactionService
import com.revolut.utils.startServer
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.restassured.RestAssured
import io.restassured.RestAssured.*
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.javamoney.moneta.Money
import org.junit.jupiter.api.*
import spark.kotlin.stop

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionControllerTest {
    private val transactionService = mockk<TransactionService>()
    private var port: Int = 0

    companion object {
        @AfterAll
        @JvmStatic
        fun afterAll() {
            stop()
        }
    }

    init {
        port = startServer {
            val injector = Guice.createInjector(TestModule(transactionService))
            injector.injectMembers(TransactionController())
        }
    }

    @Test
    fun `executeTransaction - should respond with error for negative transaction amount`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """{
                        "fromAccountId": "account-id-1",
                        "toAccountId": "account-id-2",
                        "amount": "EUR -22.58"
                    }""".trimMargin()
            )
            .port(port)
            .`when`()
            .post("/transaction")
            .then()
            .statusCode(400)
            .body("errorCode", equalTo("bad_request"))
            .body("errorMessage", equalTo("only nonzero positive amount is valid"))

        verify(exactly = 0) { transactionService.execute(any()) }
    }

    @Test
    fun `executeTransaction - should respond with error for zero transaction amount`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """{
                        "fromAccountId": "account-id-1",
                        "toAccountId": "account-id-2",
                        "amount": "EUR 0.0"
                    }""".trimMargin()
            )
            .port(port)
            .`when`()
            .post("/transaction")
            .then()
            .statusCode(400)
            .body("errorCode", equalTo("bad_request"))
            .body("errorMessage", equalTo("only nonzero positive amount is valid"))

        verify(exactly = 0) { transactionService.execute(any()) }
    }

    @Test
    fun `executeTransaction - should respond with error for unknown currency code`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """{
                        "fromAccountId": "account-id-1",
                        "toAccountId": "account-id-2",
                        "amount": "XXX 10.0"
                    }""".trimMargin()
            )
            .port(port)
            .`when`()
            .post("/transaction")
            .then()
            .statusCode(400)
            .body("errorCode", equalTo("bad_request"))
            .body("errorMessage", equalTo("unsupported currency code"))

        verify(exactly = 0) { transactionService.execute(any()) }
    }

    @Test
    fun `executeTransaction - should respond with error if fromAccountId is missing in request body`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """{
                        "toAccountId": "account-id-2",
                        "amount": "EUR 10.0"
                    }""".trimMargin()
            )
            .port(port)
            .`when`()
            .post("/transaction")
            .then()
            .statusCode(400)
            .body("errorCode", equalTo("bad_request"))
            .body("errorMessage", equalTo("missing required property 'fromAccountId'"))

        verify(exactly = 0) { transactionService.execute(any()) }
    }

    @Test
    fun `executeTransaction - should respond with error if toAccountId is missing in request body`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """{
                        "fromAccountId": "account-id-1",
                        "amount": "EUR 10.0"
                    }""".trimMargin()
            )
            .port(port)
            .`when`()
            .post("/transaction")
            .then()
            .statusCode(400)
            .body("errorCode", equalTo("bad_request"))
            .body("errorMessage", equalTo("missing required property 'toAccountId'"))

        verify(exactly = 0) { transactionService.execute(any()) }
    }

    @Test
    fun `executeTransaction - should respond with error if amount is missing in request body`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """{
                        "fromAccountId": "account-id-1",
                        "toAccountId": "account-id-1"
                    }""".trimMargin()
            )
            .port(port)
            .`when`()
            .post("/transaction")
            .then()
            .statusCode(400)
            .body("errorCode", equalTo("bad_request"))
            .body("errorMessage", equalTo("missing required property 'amount'"))

        verify(exactly = 0) { transactionService.execute(any()) }
    }

    @Test
    fun `executeTransaction - should respond with error if InvalidAccountException is thrown`() {
        val transactionRequest = TransactionRequest(
            fromAccountId = "account-id-1",
            toAccountId = "account-id-2",
            amount = Money.of(20, "EUR")
        )
        every {
            transactionService.execute(transactionRequest)
        } throws InvalidAccountException("From account is invalid")

        given()
            .contentType(ContentType.JSON)
            .body(
                """{
                        "fromAccountId": "account-id-1",
                        "toAccountId": "account-id-2",
                        "amount": "EUR 20"
                    }""".trimMargin()
            )
            .port(port)
            .`when`()
            .post("/transaction")
            .then()
            .statusCode(400)
            .body("errorCode", equalTo("bad_request"))
            .body("errorMessage", equalTo("From account is invalid"))

        // then
        verify { transactionService.execute(transactionRequest) }
    }

    @Test
    fun `executeTransaction - should respond with error if InsufficientBalanceException is thrown`() {
        val transactionRequest = TransactionRequest(
            fromAccountId = "account-id-1",
            toAccountId = "account-id-2",
            amount = Money.of(20, "EUR")
        )
        every {
            transactionService.execute(transactionRequest)
        } throws InsufficientBalanceException("Not sufficient balance to execute this transaction")

        given()
            .contentType(ContentType.JSON)
            .body(
                """{
                        "fromAccountId": "account-id-1",
                        "toAccountId": "account-id-2",
                        "amount": "EUR 20"
                    }""".trimMargin()
            )
            .port(port)
            .`when`()
            .post("/transaction")
            .then()
            .statusCode(400)
            .body("errorCode", equalTo("bad_request"))
            .body("errorMessage", equalTo("Not sufficient balance to execute this transaction"))

        // then
        verify { transactionService.execute(transactionRequest) }
    }

    @Test
    fun `executeTransaction - should return transactionId for successful transaction`() {
        val transactionRequest = TransactionRequest(
            fromAccountId = "account-id-1",
            toAccountId = "account-id-2",
            amount = Money.of(20, "EUR")
        )
        every {
            transactionService.execute(transactionRequest)
        } returns "transaction-id-1"

        given()
            .contentType(ContentType.JSON)
            .body(
                """{
                        "fromAccountId": "account-id-1",
                        "toAccountId": "account-id-2",
                        "amount": "EUR 20"
                    }""".trimMargin()
            )
            .port(port)
            .`when`()
            .post("/transaction")
            .then()
            .statusCode(201)
            .body("transactionId", equalTo("transaction-id-1"))

        // then
        verify { transactionService.execute(transactionRequest) }
    }

    private class TestModule(private val transactionService: TransactionService) : AbstractModule() {
        @Provides
        fun transactionService() = transactionService
    }
}
