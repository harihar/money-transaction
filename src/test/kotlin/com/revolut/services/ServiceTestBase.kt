package com.revolut.services

import com.revolut.configuration.TestDb
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.BeforeEach

abstract class ServiceTestBase {

    protected lateinit var dbCtx: DSLContext

    @BeforeEach
    fun setUp() {
        TestDb.initDb()
        dbCtx = DSL.using(TestDb.dataSource, SQLDialect.H2)
    }
}
