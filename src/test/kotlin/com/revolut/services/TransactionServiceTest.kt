package com.revolut.services

import com.revolut.exceptions.InsufficientBalanceException
import com.revolut.exceptions.InvalidAccountException
import com.revolut.model.TransactionRequest
import com.revolut.configuration.TestDb
import com.revolut.utils.createAccount
import com.revolut.utils.createUser
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.javamoney.moneta.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.math.BigDecimal
import java.math.RoundingMode
import javax.money.Monetary
import javax.money.convert.MonetaryConversions

class TransactionServiceTest : ServiceTestBase() {

    private lateinit var transactionService: TransactionService

    @BeforeEach
    fun beforeEach() {
        transactionService = TransactionService(TestDb.dataSource, CurrencyConverter())
    }

    @Test
    fun `execute - should throw InvalidAccountException when fromAccount does not exist in db`() {
        // given
        val userId = dbCtx.createUser("jane", "doe")
        val accountId = dbCtx.createAccount(userId, BigDecimal(0.0))

        // when
        assertThatThrownBy {
            transactionService.execute(TransactionRequest("unknownId", accountId, Money.of(1.0, "EUR")))
        }

            // then
            .isInstanceOf(InvalidAccountException::class.java)
            .hasMessage("The from account 'unknownId' is not a valid account")
    }

    @Test
    fun `execute - should throw InvalidAccountException when toAccount does not exist in db`() {
        // given
        val userId = dbCtx.createUser("jane", "doe")
        val accountId = dbCtx.createAccount(userId, BigDecimal(0.0))

        // when
        assertThatThrownBy {
            transactionService.execute(
                TransactionRequest(
                    fromAccountId = accountId,
                    toAccountId = "unknownId",
                    amount = Money.of(1, "EUR")
                )
            )
        }

            // then
            .isInstanceOf(InvalidAccountException::class.java)
            .hasMessage("The to account 'unknownId' is not a valid account")
    }

    @Test
    fun `execute - should throw InsufficientBalanceException when account balance is 0`() {
        // given
        val userId = dbCtx.createUser("john", "doe")
        val fromAccountId = dbCtx.createAccount(userId, BigDecimal(0.0))
        val toAccountId = dbCtx.createAccount(userId, BigDecimal(0.0))

        // when
        assertThatThrownBy {
            transactionService.execute(
                TransactionRequest(
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    amount = Money.of(1.0, "EUR")
                )
            )
        }

            // then
            .isInstanceOf(InsufficientBalanceException::class.java)
            .hasMessage("Not enough balance in the account to perform this transaction")
    }

    @Test
    fun `execute - should throw InsufficientBalanceException when account balance is less than transfer amount`() {
        // given
        val userId = dbCtx.createUser("john", "doe")
        val fromAccountId = dbCtx.createAccount(userId, BigDecimal(10.0))
        val toAccountId = dbCtx.createAccount(userId, BigDecimal(0.0))

        // when
        assertThatThrownBy {
            transactionService.execute(
                TransactionRequest(
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    amount = Money.of(10.1, "EUR")
                )
            )
        }

            // then
            .isInstanceOf(InsufficientBalanceException::class.java)
            .hasMessage("Not enough balance in the account to perform this transaction")
    }

    @Test
    fun `execute - should not throw InsufficientBalanceException when account balance is more than transfer amount`() {
        // given
        val userId = dbCtx.createUser("john", "doe")
        val fromAccountId = dbCtx.createAccount(userId, BigDecimal(2.0))
        val toAccountId = dbCtx.createAccount(userId, BigDecimal(0.0))

        // when and then
        assertDoesNotThrow {
            transactionService.execute(
                TransactionRequest(
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    amount = Money.of(1.0, "EUR")
                )
            )
        }
    }

    @Test
    fun `execute - should increase the balance of the to account by the transfer amount`() {
        // given
        val userId = dbCtx.createUser("john", "doe")
        val fromAccountId = dbCtx.createAccount(userId, BigDecimal(12.0))
        val toAccountId = dbCtx.createAccount(userId, BigDecimal(0.0))

        // when
        transactionService.execute(
            TransactionRequest(
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amount = Money.of(1.0, "EUR")
            )
        )

        // then
        dbCtx.fetchOne("select balance from accounts where id='$toAccountId'")
            .run {
                assertThat(this.get(0) as BigDecimal).isEqualTo("1.0000")
            }
    }

    @Test
    fun `execute - should reduce the balance of the from account by the transfer amount`() {
        // given
        val userId = dbCtx.createUser("john", "doe")
        val fromAccountId = dbCtx.createAccount(userId, BigDecimal(12.0))
        val toAccountId = dbCtx.createAccount(userId, BigDecimal(0.0))

        // when
        transactionService.execute(
            TransactionRequest(
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amount = Money.of(1.0, "EUR")
            )
        )

        // then
        dbCtx.fetchOne("select balance from accounts where id='$fromAccountId'")
            .run {
                assertThat(this.get(0) as BigDecimal).isEqualTo("11.0000")
            }
    }

    @Test
    fun `execute - should reduce the balance of the from account by the transfer amount with proper currency conversion`() {
        // given
        val userId = dbCtx.createUser("john", "doe")
        val fromAccountId = dbCtx.createAccount(userId, BigDecimal(12.0))
        val toAccountId = dbCtx.createAccount(userId, BigDecimal(0.0))
        val euroCurrency = MonetaryConversions.getConversion(Monetary.getCurrency("EUR"))

        // when
        transactionService.execute(
            TransactionRequest(
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amount = Money.of(1.0, "USD")
            )
        )

        // then
        dbCtx.fetchOne("select balance from accounts where id='$fromAccountId'")
            .run {
                assertThat(this.get(0) as BigDecimal).isEqualTo(
                    Money.of(12.0, "EUR")
                        .subtract(
                            Money.of(1.0, "USD").with(euroCurrency)
                        ).numberStripped.setScale(4, RoundingMode.HALF_DOWN)
                )
            }
    }

    @Test
    fun `execute - should create a transaction record and return the id of the created transaction`() {
        // given
        val userId = dbCtx.createUser("john", "doe")
        val fromAccountId = dbCtx.createAccount(userId, BigDecimal(12.0))
        val toAccountId = dbCtx.createAccount(userId, BigDecimal(0.0))

        // when
        val transactionId = transactionService.execute(
            TransactionRequest(
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amount = Money.of(1.0, "USD")
            )
        )

        // then
        dbCtx.fetchOne("select amount, currency_code, from_account, to_account from transactions where id='$transactionId'")
            .run {
                assertThat(this.get(0) as BigDecimal).isEqualTo("1.00")
                assertThat(this.get(1) as String).isEqualTo("USD")
                assertThat(this.get(2) as String).isEqualTo(fromAccountId)
                assertThat(this.get(3) as String).isEqualTo(toAccountId)
            }
    }
}
