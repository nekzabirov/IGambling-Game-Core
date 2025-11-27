import infrastructure.aggregator.aggregatorRoute
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    System.setProperty("user.timezone", "UTC")
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))

    embeddedServer(
        CIO,
        port = 80,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    install(Koin) {
        slf4jLogger()
        modules(sharedModule)
    }

    install(SharedPlugin) {
        databaseUrl = System.getenv("DATABASE_URL")
        databaseDriver = "org.postgresql.Driver"
        databaseUser = System.getenv("DATABASE_USER")
        databasePassword = System.getenv("DATABASE_PASSWORD")
        autoCreateSchema = true
        showSql = false
    }

    install(ContentNegotiation) {
        json()
    }

    routing {
        route("webhook") {
            aggregatorRoute()
        }
    }
}
