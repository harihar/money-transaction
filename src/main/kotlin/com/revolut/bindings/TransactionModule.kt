package com.revolut.bindings

import com.google.inject.Exposed
import com.google.inject.PrivateModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.revolut.services.CurrencyConverter
import com.revolut.services.TransactionService
import javax.sql.DataSource

class TransactionModule : PrivateModule() {
    override fun configure() {
    }

    @Provides
    @Singleton
    fun currencyConverter(): CurrencyConverter {
        return CurrencyConverter()
    }

    @Provides
    @Singleton
    @Exposed
    fun transactionService(dataSource: DataSource, currencyConverter: CurrencyConverter): TransactionService {
        return TransactionService(dataSource, currencyConverter)
    }
}
