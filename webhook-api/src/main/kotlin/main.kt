import infrastructure.aggregator.onegamehub.handler.OneGameHubHandler
import infrastructure.aggregator.onegamehub.handler.error.OneGameHubError
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(
        CIO,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        exception<OneGameHubError> { call, cause ->
            call.respond(cause.status, cause.body)
        }
    }

    routing {
        route("webhook") {
            OneGameHubHandler.makeRoute(this)
        }
    }
}
