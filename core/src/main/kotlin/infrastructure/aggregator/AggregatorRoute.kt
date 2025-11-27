package infrastructure.aggregator

import infrastructure.aggregator.onegamehub.hook.oneGameHubRoute
import io.ktor.server.routing.*

fun Route.aggregatorRoute() = route("/{aggregatorIdentity}") {
    oneGameHubRoute()
}