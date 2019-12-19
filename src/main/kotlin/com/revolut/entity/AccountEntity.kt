package com.revolut.entity

import org.javamoney.moneta.Money
import org.jooq.DSLContext
import org.jooq.impl.DSL

data class AccountEntity(val id: String, val userId: String, val balance: Money)

fun AccountEntity.updateAccount(dslContext: DSLContext) {
    dslContext.update(DSL.table("accounts"))
        .set(DSL.field("balance"), balance.numberStripped)
        .where(DSL.field("id").eq(id))
        .execute()
}
