package com.revolut.utils

import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.math.BigDecimal

fun DSLContext.createUser(firstName: String, lastName: String): String {
    val userId = randomUUIDString()
    this
        .insertInto(DSL.table("users"))
        .columns(DSL.field("id"), DSL.field("first_name"), DSL.field("last_name"))
        .values(userId, firstName, lastName)
        .execute()
    return userId
}

fun DSLContext.createAccount(userId: String, balance: BigDecimal): String {
    val accountId = randomUUIDString()
    this
        .insertInto(DSL.table("accounts"))
        .columns(DSL.field("id"), DSL.field("user_id"), DSL.field("balance"))
        .values(accountId, userId, balance)
        .execute()
    return accountId
}
