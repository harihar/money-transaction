package com.revolut.configuration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import javax.sql.DataSource

class TestDb {
    companion object {
        val dataSource: DataSource by lazy {
            HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = "jdbc:h2:mem:test"
                    username = "sa"
                    maximumPoolSize = 25
                    connectionTimeout = 2000
                }
            )
        }

        fun initDb() {
            Flyway(FluentConfiguration().dataSource(dataSource)).run {
                clean()
                migrate()
            }
        }
    }
}
