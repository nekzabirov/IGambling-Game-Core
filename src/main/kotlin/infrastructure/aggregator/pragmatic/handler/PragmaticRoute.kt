package infrastructure.aggregator.pragmatic.handler

import infrastructure.aggregator.pragmatic.handler.dto.PragmaticBetDto
import infrastructure.aggregator.pragmatic.handler.dto.PragmaticResponse
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import shared.value.SessionToken

internal fun Route.pragmaticWebhookRoute() {
    val handler by application.inject<PragmaticHandler>()

    post("/pragmatic") {
        // TODO: Implement Pragmatic webhook route based on their API specification
        // Parse action and session token from request
        // Route to appropriate handler method

        call.respond(PragmaticResponse.Error.PragmaticInvalidRequest)
    }
}
