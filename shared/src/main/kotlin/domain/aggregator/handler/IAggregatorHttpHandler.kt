package domain.aggregator.handler

import io.ktor.server.routing.*

interface IAggregatorHttpHandler {
    fun makeRoute(route: Route): Route
}