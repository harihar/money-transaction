package com.revolut.services

import com.revolut.model.Account
import org.javamoney.moneta.Money
import org.jooq.impl.DSL.*
import java.math.BigDecimal
import javax.sql.DataSource

class AccountService(
    private val dataSource: DataSource
) {
    fun getAccounts(): List<Account> {
        return using(dataSource.connection)
            .select(field("id"), field("balance"))
            .from(table("accounts"))
            .fetch()?.map {
                Account(
                    accountId = it[0] as String,
                    balance = Money.of(it[1] as BigDecimal, "EUR")
                )
            } ?: emptyList()
    }

    fun getAccount(accountId: String): Account? {
        return using(dataSource.connection)
            .select(field("balance"))
            .from(table("accounts"))
            .where(field("id").eq(accountId))
            .fetchOne()?.map {
                Account(
                    accountId = accountId,
                    balance = Money.of(it[0] as BigDecimal, "EUR")
                )
            }
    }
}

