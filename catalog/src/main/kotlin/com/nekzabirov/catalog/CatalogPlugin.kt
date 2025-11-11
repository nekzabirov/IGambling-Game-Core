package com.nekzabirov.catalog

import com.nekzabirov.catalog.table.*
import io.ktor.network.sockets.Connection
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction

class PluginConfig {
    // Database configuration
    var databaseUrl: String = System.getenv("DATABASE_URL") ?: "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    var databaseDriver: String = System.getenv("DATABASE_DRIVER") ?: "org.h2.Driver"
    var databaseUser: String = System.getenv("DATABASE_USER") ?: "root"
    var databasePassword: String = System.getenv("DATABASE_PASSWORD") ?: ""
    var autoCreateSchema: Boolean = true
    var showSql: Boolean = false
}

val CatalogPlugin = createApplicationPlugin(name = "CatalogPlugin", createConfiguration = ::PluginConfig) {
    Database.connect(
        url = pluginConfig.databaseUrl,
        user = pluginConfig.databaseUser,
        driver = pluginConfig.databaseDriver,
        password = pluginConfig.databasePassword,
    )

    transaction {
        create(
            GameTable,
            GameVariantTable,
            ProviderTable,
            CollectionTable,
            CollectionGameTable,
            GameFavouriteTable,
            AggregatorInfoTable
        )
    }
}