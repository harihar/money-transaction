package com.revolut

import com.google.inject.Guice
import com.revolut.bindings.AccountsModule
import com.revolut.bindings.CommonModule
import com.revolut.bindings.TransactionModule
import com.revolut.controller.AccountsController
import com.revolut.controller.TransactionController
import org.flywaydb.core.Flyway
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import javax.sql.DataSource

val logger = LoggerFactory.getLogger("com.revolut.Main")

fun main() {
    logger.info("Starting main")
    val injector = Guice.createInjector(
        CommonModule(),
        AccountsModule(),
        TransactionModule()
    )
    injector.injectMembers(AccountsController())
    injector.injectMembers(TransactionController())

    injector.getInstance(Flyway::class.java).migrate()

    insertSeedDataInDB(injector.getInstance(DataSource::class.java))
}

private fun insertSeedDataInDB(dataSource: DataSource) {
    logger.info("==Starting seed data load==")
    val accountsSeedDataSql = object {}.javaClass
        .classLoader
        .getResource("db.seed/accounts.sql")?.readText()
    DSL.using(dataSource.connection)
        .execute(accountsSeedDataSql)
    logger.info("==Finished seed data loading==")
}
