package com.revolut.services

import com.revolut.model.TransactionRequest
import com.revolut.configuration.TestDb
import com.revolut.utils.createAccount
import com.revolut.utils.createUser
import org.assertj.core.api.Assertions.assertThat
import org.javamoney.moneta.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.concurrent.*

class TransactionServiceConcurrencyTest : ServiceTestBase() {

    private lateinit var transactionService: TransactionService

    @BeforeEach
    fun beforeEach() {
        transactionService = TransactionService(TestDb.dataSource, CurrencyConverter())
    }

    @Test
    fun `execute - handle concurrent request in a serializable fashion`() {
        // given
        val userId = dbCtx.createUser("john", "doe")
        val fromAccountId = dbCtx.createAccount(userId, BigDecimal(12.0))
        val toAccountId = dbCtx.createAccount(userId, BigDecimal(0.0))
        val transactionRequests = (1..10).map {
            Callable {
                transactionService.execute(
                    TransactionRequest(
                        fromAccountId = fromAccountId,
                        toAccountId = toAccountId,
                        amount = Money.of(1.0, "EUR")
                    )
                )
            }
        }
        val executorService = Executors.newFixedThreadPool(5)

        // when
        val futureResults = executorService.invokeAll(transactionRequests, 5, TimeUnit.SECONDS)

        // then
        // verify toAccount balance
        dbCtx.fetchOne("select balance from accounts where id='$toAccountId'")
            .run {
                assertThat(this.get(0) as BigDecimal).isEqualTo("10.0000")
            }
        // verify fromAccount balance
        dbCtx.fetchOne("select balance from accounts where id='$fromAccountId'")
            .run {
                assertThat(this.get(0) as BigDecimal).isEqualTo("2.0000")
            }
        // verify all transactions
        futureResults.forEach {
            dbCtx.fetchOne("select amount, currency_code, from_account, to_account from transactions where id='${it.get()}'")
                .run {
                    assertThat(this.get(0) as BigDecimal).isEqualTo("1.00")
                    assertThat(this.get(1) as String).isEqualTo("EUR")
                    assertThat(this.get(2) as String).isEqualTo(fromAccountId)
                    assertThat(this.get(3) as String).isEqualTo(toAccountId)
                }
        }
    }
}
