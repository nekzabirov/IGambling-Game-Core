import io.ktor.server.application.*
import io.ktor.server.testing.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import java.util.*

abstract class BaseTest {

    fun doTest(block: suspend Application.() -> Unit) = testApplication {
        // Set timezone before any database operations
        System.setProperty("user.timezone", "UTC")
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        application {
            install(Koin) {
                slf4jLogger()
            }

            block()
        }
    }

}