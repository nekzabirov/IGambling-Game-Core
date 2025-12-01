package com.nekgamebling.infrastructure.aggregator.onegamehub.webhook

import io.ktor.http.Parameters
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

private val Parameters.amount get() = this["amount"]!!.toInt()
private val Parameters.gameSymbol get() = this["game_id"]!!
private val Parameters.transactionId get() = this["transaction_id"]!!
private val Parameters.roundId get() = this["round_id"]!!
private val Parameters.freespinId get() = this["freerounds_id"]
private val Parameters.isRoundEnd get() = this["ext_round_finished"] == "1"

internal fun Route.oneGameHubWebhookRoute() = post("/onegamehub") {
    val action = call.queryParameters["action"]
    val sessionToken = call.queryParameters["extra"]

}