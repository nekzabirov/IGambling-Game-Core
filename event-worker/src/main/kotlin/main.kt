import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory

private val logger by lazy {
    LoggerFactory.getLogger("SyncGamesJob")
}

fun main() {
    // Set timezone before any database operations
    System.setProperty("user.timezone", "UTC")
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))

    embeddedServer(CIO, port = 0, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(Koin) {
        slf4jLogger()
    }

    install(SharedPlugin) {
        databaseUrl = System.getenv("DATABASE_URL")
        databaseDriver = "org.postgresql.Driver"
        databaseUser = System.getenv("DATABASE_USER")
        databasePassword = System.getenv("DATABASE_PASSWORD")
        autoCreateSchema = true
        showSql = false
    }

    consumeSpinSettle(System.getenv("RABBITMQ_EXCHANGE"))
}