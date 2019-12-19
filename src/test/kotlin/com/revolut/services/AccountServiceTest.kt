package com.revolut.services

import com.revolut.model.Account
import com.revolut.configuration.TestDb
import com.revolut.utils.createAccount
import com.revolut.utils.createUser
import org.assertj.core.api.Assertions.assertThat
import org.javamoney.moneta.Money
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AccountServiceTest : ServiceTestBase() {

    lateinit var accountService: AccountService

    @BeforeEach
    fun beforeEach() {
        accountService = AccountService(TestDb.dataSource)
    }

    @Test
    fun `getAccounts - should return an empty list if no accounts exist`() {
        // given

        // when
        val accounts = accountService.getAccounts()

        // then
        assertThat(accounts).isEqualTo(emptyList<Account>())
    }

    @Test
    fun `getAccounts - should return list of all accounts with balance`() {
        // given
        val userId1 = dbCtx.createUser("john", "doe")
        val userId2 = dbCtx.createUser("jane", "doe")
        val accountId1 = dbCtx.createAccount(userId1, BigDecimal(12.5839))
        val accountId2 = dbCtx.createAccount(userId2, BigDecimal(450000.7500))

        // when
        val accounts = accountService.getAccounts()

        // then
        assertThat(accounts).isEqualTo(
            listOf(
                Account(accountId1, Money.of(12.5839, "EUR")),
                Account(accountId2, Money.of(450000.7500, "EUR"))
            )
        )
    }

    @Test
    fun `getAccount - should return null if account does not exist`() {
        // when
        val account = accountService.getAccount("non-existent-account-id")

        // then
        assertThat(account).isNull()
    }

    @Test
    fun `getAccount - should return the account with balance`() {
        // given
        val userId1 = dbCtx.createUser("john", "doe")
        val accountId1 = dbCtx.createAccount(userId1, BigDecimal(12.5839))

        // when
        val account = accountService.getAccount(accountId1)

        // then
        assertThat(account).isEqualTo(
            Account(accountId1, Money.of(12.5839, "EUR"))
        )
    }
}
