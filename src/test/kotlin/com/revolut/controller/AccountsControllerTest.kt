package com.revolut.controller

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Provides
import com.revolut.model.Account
import com.revolut.services.AccountService
import com.revolut.utils.startServer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.*
import org.javamoney.moneta.Money
import org.junit.jupiter.api.*
import spark.kotlin.stop

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountsControllerTest {
    private val accountService = mockk<AccountService>()
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
            val injector = Guice.createInjector(TestModule(accountService))
            injector.injectMembers(AccountsController())
        }
    }

    @Test
    fun `getAccounts - should return the list of available accounts with balance`() {
        // given
        val listOfAccounts = listOf(
            Account(
                accountId = "account-id-1",
                balance = Money.of(25.4567, "EUR")
            ),
            Account(
                accountId = "account-id-2",
                balance = Money.of(254.567, "USD")
            )
        )
        every { accountService.getAccounts() } returns listOfAccounts

        given()
            .port(port)
            .`when`()
            .get("/accounts")
            .then()
            .statusCode(200)
            .body("accounts.size()", equalTo(2))
            .body("accounts[0].accountId", equalTo("account-id-1"))
            .body("accounts[0].balance", equalTo("EUR 25.4567"))
            .body("accounts[1].accountId", equalTo("account-id-2"))
            .body("accounts[1].balance", equalTo("USD 254.567"))

        verify { accountService.getAccounts() }
    }

    @Test
    fun `getAccount - should 404 if the account does not exist`() {
        // given
        every { accountService.getAccount("account-id-1") } returns null

        given()
            .port(port)
            .`when`()
            .get("/accounts/account-id-1")
            .then()
            .statusCode(404)

        verify { accountService.getAccount("account-id-1") }
    }

    @Test
    fun `getAccount - should return the account with balance`() {
        // given
        every { accountService.getAccount("account-id-1") } returns Account(
            accountId = "account-id-1",
            balance = Money.of(25.4567, "EUR")
        )

        given()
            .port(port)
            .`when`()
            .get("/accounts/account-id-1")
            .then()
            .statusCode(200)
            .body("accountId", equalTo("account-id-1"))
            .body("balance", equalTo("EUR 25.4567"))

        verify { accountService.getAccount("account-id-1") }
    }

    private class TestModule(private val accountService: AccountService) : AbstractModule() {
        @Provides
        fun transactionService() = accountService
    }
}

