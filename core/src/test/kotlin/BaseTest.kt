import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.test.runTest
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import java.util.*
import kotlin.time.Duration.Companion.minutes

abstract class BaseTest {

    fun doTest(block: suspend Application.() -> Unit) = runTest(timeout = 5.minutes) {
        // Set timezone before any database operations
        System.setProperty("user.timezone", "UTC")
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        val server = embeddedServer(CIO, port = 0) {
            install(Koin) {
                slf4jLogger()
            }

            install(SharedPlugin) {
                databaseUrl = "jdbc:postgresql://localhost:5432/mydb?TimeZone=UTC"
                databaseDriver = "org.postgresql.Driver"
                databaseUser = "user"
                databasePassword = "password"
                autoCreateSchema = true
                showSql = true
            }
            
            block()
        }
        
        server.start(wait = false)
        
        try {
            // Ждем пока сервер запустится
            Thread.sleep(100)
        } finally {
            server.stop(0, 0)
        }
    }
}