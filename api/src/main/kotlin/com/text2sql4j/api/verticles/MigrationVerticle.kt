package com.text2sql4j.api.verticles

import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration

class MigrationVerticle(
    private val databaseServer: String,
    private val databaseName: String,
    private val databaseUsername: String,
    private val databasePassword: String
) : CoroutineVerticle() {
    override suspend fun start() {
        val flyway = Flyway(
            FluentConfiguration().dataSource(
                "jdbc:postgresql://$databaseServer/$databaseName",
                databaseUsername,
                databasePassword
            )
        )
        flyway.migrate()
    }
}