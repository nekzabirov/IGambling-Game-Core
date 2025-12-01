package infrastructure.aggregator.onegamehub.handler

import infrastructure.aggregator.onegamehub.handler.dto.OneGameHubResponse
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import shared.value.SessionToken
import kotlin.getValue

private val Parameters.amount get() = this["amount"]!!.toInt()
private val Parameters.gameSymbol get() = this["game_id"]!!
private val Parameters.transactionId get() = this["transaction_id"]!!
private val Parameters.roundId get() = this["round_id"]!!
private val Parameters.freespinId get() = this["freerounds_id"]
private val Parameters.isRoundEnd get() = this["ext_round_finished"] == "1"

internal fun Route.oneGameHubWebhookRoute() {
    val handler by application.inject<OneGameHubHandler>()

    post("/onegamehub") {
        val action = call.queryParameters["action"]
        val sessionToken = call.queryParameters["extra"]

        if (action == null || sessionToken == null) {
            call.respond(OneGameHubResponse.Error.OneGameHubInvalidateRequest)
            return@post
        }

        val mainToken = SessionToken(sessionToken)

        val response = when (action) {
            "balance" -> handler.balance(mainToken)
            else -> OneGameHubResponse.Error.OneGameHubInvalidateRequest
        }

        call.respond(response)
    }
}