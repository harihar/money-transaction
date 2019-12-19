package com.revolut.bindings

import com.google.inject.*
import com.revolut.controller.AccountsController
import com.revolut.services.AccountService
import javax.sql.DataSource

class AccountsModule : PrivateModule() {

    override fun configure() {
    }

    @Provides
    @Singleton
    @Exposed
    fun accountService(dataSource: DataSource): AccountService {
        return AccountService(dataSource)
    }

}
