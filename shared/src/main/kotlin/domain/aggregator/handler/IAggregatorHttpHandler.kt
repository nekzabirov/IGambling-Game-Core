package domain.aggregator.handler

import io.ktor.server.routing.*

interface IAggregatorHttpHandler {
    fun Route.route(): Route
}