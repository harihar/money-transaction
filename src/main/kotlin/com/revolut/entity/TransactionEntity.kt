package com.revolut.entity

import com.revolut.utils.randomUUIDString
import org.javamoney.moneta.Money
import org.jooq.DSLContext
import org.jooq.impl.DSL

data class TransactionEntity(val id: String = "", val amount: Money, val fromAccount: String, val toAccount: String)

fun TransactionEntity.store(dslContext: DSLContext): TransactionEntity {
    val uuid = randomUUIDString()
    dslContext.insertInto(DSL.table("transactions"))
        .columns(
            DSL.field("id"),
            DSL.field("amount"),
            DSL.field("currency_code"),
            DSL.field("from_account"),
            DSL.field("to_account")
        )
        .values(
            uuid, this.amount.numberStripped, this.amount.currency.currencyCode, this.fromAccount, this.toAccount
        )
        .execute()
    return this.copy(id = uuid)
}
