import domain.aggregator.table.AggregatorInfoTable
import domain.collection.table.CollectionGameTable
import domain.collection.table.CollectionTable
import domain.game.table.GameFavouriteTable
import domain.game.table.GameTable
import domain.game.table.GameVariantTable
import domain.provider.table.ProviderTable
import domain.session.table.RoundTable
import domain.session.table.SessionTable
import domain.session.table.SpinTable
import io.ktor.server.application.*
import io.ktor.util.AttributeKey
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction

class PluginConfig {
    data class RabbitMqConfig(
        val url: String,
        val exchange: String
    )

    // Database configuration
    var databaseUrl: String = System.getenv("DATABASE_URL") ?: "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    var databaseDriver: String = System.getenv("DATABASE_DRIVER") ?: "org.h2.Driver"
    var databaseUser: String = System.getenv("DATABASE_USER") ?: "root"
    var databasePassword: String = System.getenv("DATABASE_PASSWORD") ?: ""
    var autoCreateSchema: Boolean = true
    var showSql: Boolean = false

    var rabbitMq: RabbitMqConfig = RabbitMqConfig(
        url = System.getenv("RABBITMQ_URL") ?: "amqp://localhost:5672",
        exchange = System.getenv("RABBITMQ_EXCHANGE") ?: "game.event"
    )
}

internal val SharedPluginConfigAttribute = AttributeKey<PluginConfig>("SharedPluginConfig")

val SharedPlugin = createApplicationPlugin(name = "SharedPlugin", createConfiguration = ::PluginConfig) {
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
            AggregatorInfoTable,
            SessionTable,
            RoundTable,
            SpinTable
        )
    }

    application.attributes.put(SharedPluginConfigAttribute, pluginConfig)
}

internal val Application.sharedPluginConfig: PluginConfig get() = attributes[SharedPluginConfigAttribute]