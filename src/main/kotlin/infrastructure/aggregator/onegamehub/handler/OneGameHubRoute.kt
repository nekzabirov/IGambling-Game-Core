package infrastructure.aggregator.onegamehub.handler

import com.nekgamebling.infrastructure.aggregator.onegamehub.handler.dto.OneGameHubBetDto
import infrastructure.aggregator.onegamehub.handler.dto.OneGameHubResponse
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import shared.value.SessionToken
import kotlin.getValue

private val Parameters.amount get() = this["amount"]!!.toBigInteger()
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
            call.respond(OneGameHubResponse.Error.UNEXPECTED_ERROR)
            return@post
        }

        val mainToken = SessionToken(sessionToken)

        val response = when (action) {
            "balance" -> handler.balance(mainToken)

            "bet" -> handler.bet(
                mainToken,
                payload = OneGameHubBetDto(
                    gameSymbol = call.parameters.gameSymbol,
                    roundId = call.parameters.roundId,
                    transactionId = call.parameters.transactionId,
                    freeSpinId = call.parameters.freespinId,
                    amount = call.parameters.amount
                )
            )

            "win" -> handler.win(mainToken, payload = OneGameHubBetDto(
                gameSymbol = call.parameters.gameSymbol,
                roundId = call.parameters.roundId,
                transactionId = call.parameters.transactionId,
                freeSpinId = call.parameters.freespinId,
                amount = call.parameters.amount,
                finishRound = call.parameters.isRoundEnd
            ))

            else -> OneGameHubResponse.Error.UNEXPECTED_ERROR
        }

        call.respond(response)
    }
}