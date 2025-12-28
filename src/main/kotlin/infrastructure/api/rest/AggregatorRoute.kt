package api.rest

import infrastructure.aggregator.onegamehub.handler.oneGameHubWebhookRoute
import io.ktor.server.routing.*

/**
 * Aggregator webhook routes for handling callbacks from game aggregators.
 */
fun Route.aggregatorRoute() = route("/{aggregatorIdentity}") {
    oneGameHubWebhookRoute()
    // TODO: Add pateplayWebhookRoute() when handler is implemented
}
