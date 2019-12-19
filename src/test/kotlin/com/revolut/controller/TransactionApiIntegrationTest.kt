package com.revolut.controller

import com.revolut.main
import com.revolut.utils.startServer
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import spark.kotlin.stop
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionApiIntegrationTest {
    private var port: Int = 0

    companion object {
        private val log = LoggerFactory.getLogger(TransactionApiIntegrationTest::class.java)

        @AfterAll
        @JvmStatic
        fun afterAll() {
            stop()
        }
    }

    init {
        port = startServer {
            main()
        }
    }

    // This test runs against the seed data loaded at application-start
    // from src/main/resources/db.seed/accounts.sql
    @Test
    fun `perform one transaction and verify balance afterwards`() {
        val fromAccountId = "70feaa45-1307-4556-8a27-a431f8935f4d"
        val toAccountId = "2117a2ee-638c-4325-b523-fcbf34f6066a"

        val transactionId = given()
            .contentType(ContentType.JSON)
            .body(
                """{
                        "fromAccountId": "$fromAccountId",
                        "toAccountId": "$toAccountId",
                        "amount": "EUR 200"
                    }""".trimMargin()
            )
            .port(port)
            .`when`()
            .post("/transaction")
            .then()
            .statusCode(201)
            .extract().asString()

        log.info("Transaction ID is: $transactionId")

        // verify accounts after transaction
        // verify from account balance
        given()
            .port(port)
            .`when`()
            .get("/accounts/$fromAccountId")
            .then()
            .statusCode(200)
            .body("balance", Matchers.equalTo("EUR 4800"))

        // verify to account balance
        given()
            .port(port)
            .`when`()
            .get("/accounts/$toAccountId")
            .then()
            .statusCode(200)
            .body("balance", Matchers.equalTo("EUR 2200.45"))
    }

    // This test runs against the seed data loaded at application-start
    // from src/main/resources/db.seed/accounts.sql
    @Test
    fun `perform 10 transactions in parallel between 2 accounts and verify account balance afterwards`() {
        val fromAccountId = "6f1d3f64-ad25-45c6-ad35-e221f995da34"
        val toAccountId = "30a15847-31da-4c15-a475-bba1aea60cd6"

        val transactionRequests = (1..10).map {
            Callable {
                given()
                    .contentType(ContentType.JSON)
                    .body(
                        """{
                        "fromAccountId": "$fromAccountId",
                        "toAccountId": "$toAccountId",
                        "amount": "EUR 100"
                    }""".trimMargin()
                    )
                    .port(port)
                    .`when`()
                    .post("/transaction")
                    .then()
                    .statusCode(201)
                    .extract().asString()
            }
        }
        val executorService = Executors.newFixedThreadPool(10)
        val futureResults = executorService.invokeAll(transactionRequests, 5, TimeUnit.SECONDS)
        log.info("Transaction Ids are: ${futureResults.map { it.get() }}")

        // verify accounts after transaction
        // verify from account balance
        given()
            .port(port)
            .`when`()
            .get("/accounts/$fromAccountId")
            .then()
            .statusCode(200)
            .body("balance", Matchers.equalTo("EUR 11000.3"))

        // verify to account balance
        given()
            .port(port)
            .`when`()
            .get("/accounts/$toAccountId")
            .then()
            .statusCode(200)
            .body("balance", Matchers.equalTo("EUR 1900"))
    }

}
