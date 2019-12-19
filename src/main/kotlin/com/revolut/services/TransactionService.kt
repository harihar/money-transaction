package com.revolut.services

import com.revolut.entity.AccountEntity
import com.revolut.entity.TransactionEntity
import com.revolut.entity.store
import com.revolut.entity.updateAccount
import com.revolut.exceptions.InsufficientBalanceException
import com.revolut.exceptions.InvalidAccountException
import com.revolut.model.TransactionRequest
import org.javamoney.moneta.Money
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.table
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import javax.sql.DataSource

class TransactionService(
    private val dataSource: DataSource,
    private val currencyConverter: CurrencyConverter
) {
    companion object {
        private val log = LoggerFactory.getLogger(TransactionService::class.java)
    }

    fun execute(transactionRequest: TransactionRequest): String {
        return DSL.using(dataSource.connection)
            .transactionResult { config: Configuration ->
                val dbCtx = config.dsl()

                lockAccountsTableForUpdate(dbCtx, transactionRequest)

                val fromAccount = findAccount(dbCtx, transactionRequest.fromAccountId)
                    ?: throw InvalidAccountException("The from account '${transactionRequest.fromAccountId}' is not a valid account")

                val toAccount = findAccount(dbCtx, transactionRequest.toAccountId)
                    ?: throw InvalidAccountException("The to account '${transactionRequest.toAccountId}' is not a valid account")

                val normalizedTransactionRequest = normalizeTransactionRequest(transactionRequest)

                if (fromAccount.balance < normalizedTransactionRequest.amount) {
                    throw InsufficientBalanceException("Not enough balance in the account to perform this transaction")
                }

                updateAccountBalance(dbCtx, toAccount, normalizedTransactionRequest.amount)

                updateAccountBalance(dbCtx, fromAccount, normalizedTransactionRequest.amount.negate())

                val storedTransactionEntity = storeTransactionEntity(transactionRequest, dbCtx)
                log.logIfDebug("Stored transaction entity $storedTransactionEntity")

                storedTransactionEntity.id
            }
    }

    private fun lockAccountsTableForUpdate(
        dbCtx: DSLContext,
        transactionRequest: TransactionRequest
    ) {
        log.logIfDebug("Executing transaction $transactionRequest")
        dbCtx.select()
            .from(table("accounts"))
            .where(field("id").eq(transactionRequest.fromAccountId).or(field("id").eq(transactionRequest.toAccountId)))
            .forUpdate()
            .fetch()
        log.logIfDebug("Acquired lock for executing transaction $transactionRequest")
    }

    private fun storeTransactionEntity(
        transactionRequest: TransactionRequest,
        dbCtx: DSLContext
    ): TransactionEntity {
        return TransactionEntity(
            amount = transactionRequest.amount,
            fromAccount = transactionRequest.fromAccountId,
            toAccount = transactionRequest.toAccountId
        ).store(dbCtx)
    }

    private fun normalizeTransactionRequest(transactionRequest: TransactionRequest): TransactionRequest {
        return transactionRequest.copy(
            amount = currencyConverter.convert(transactionRequest.amount)
        )
    }

    private fun updateAccountBalance(
        dbCtx: DSLContext,
        account: AccountEntity,
        deltaAmount: Money
    ) {
        val updatedFromAccount = account.copy(
            balance = account.balance.add(deltaAmount)
        )
        updatedFromAccount.updateAccount(dbCtx)
    }

    private fun findAccount(dslContext: DSLContext, accountId: String): AccountEntity? {
        return dslContext.select(field("user_id"), field("balance"))
            .from(table("accounts"))
            .where(field("id").eq(accountId))
            .fetchOne()?.map {
                AccountEntity(
                    id = accountId,
                    userId = it[0] as String,
                    balance = Money.of(it[1] as BigDecimal, "EUR")
                )
            }
    }

}

private fun Logger.logIfDebug(message: String) {
    if (isDebugEnabled) {
        this.debug(message)
    }
}

