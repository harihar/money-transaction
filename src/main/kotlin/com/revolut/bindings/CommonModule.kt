package com.revolut.bindings

import com.google.inject.*
import com.revolut.services.TransactionService
import com.revolut.services.CurrencyConverter
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import javax.sql.DataSource

class CommonModule : PrivateModule() {
    override fun configure() {
    }

    @Provides
    @Singleton
    @Exposed
    fun datasource(): DataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:revolut"
            username = "sa"
            maximumPoolSize = 20
        }
    )

    @Provides
    @Singleton
    @Exposed
    fun flyway(dataSource: DataSource) = Flyway(FluentConfiguration().dataSource(dataSource))

}
