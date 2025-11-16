package infrastructure.aggregator.onegamehub.handler

import app.service.SpinService
import core.model.Balance
import core.value.Currency
import core.value.SessionToken
import domain.aggregator.handler.IAggregatorHttpHandler
import infrastructure.aggregator.onegamehub.adapter.OneGameHubCurrencyAdapter
import infrastructure.aggregator.onegamehub.handler.error.OneGameHubError
import infrastructure.aggregator.onegamehub.handler.error.OneGameHubInvalidateRequest
import infrastructure.aggregator.onegamehub.handler.error.OneGameHubTokenExpired
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.component.KoinComponent

private val Parameters.amount get() = this["amount"]!!.toInt()
private val Parameters.gameSymbol get() = this["game_id"]!!
private val Parameters.transactionId get() = this["transaction_id"]!!
private val Parameters.roundId get() = this["round_id"]!!
private val Parameters.freespinId get() = this["freerounds_id"]
private val Parameters.isRoundEnd get() = this["ext_round_finished"] == "1"

object OneGameHubHandler : IAggregatorHttpHandler, KoinComponent {
    override fun makeRoute(route: Route) = route.post("/onegamehub") {
        val action = call.parameters["action"] ?: throw OneGameHubInvalidateRequest()
        val sessionToken = call.parameters["extra"] ?: throw OneGameHubInvalidateRequest()

        when (action) {
            "balance" -> {
                balance(SessionToken(sessionToken))
            }

            "bet" -> {
                bet(SessionToken(sessionToken))
            }

            "win" -> {
                win(SessionToken(sessionToken))
            }

            else -> {
                throw OneGameHubInvalidateRequest()
            }
        }
    }

    private suspend fun RoutingContext.balance(token: SessionToken) {
        val balance = SpinService.findBalance(token).getOrElse {
            throw OneGameHubTokenExpired()
        }

        call.respondSuccess(balance)
    }

    private suspend fun RoutingContext.bet(token: SessionToken) {
        val balance = SpinService.place(
            token = token,
            gameSymbol = call.parameters.gameSymbol,
            extRoundId = call.parameters.roundId,
            transactionId = call.parameters.transactionId,
            amount = call.parameters.amount
        ).getOrElse {
            throw OneGameHubError.transform(it)
        }

        call.respondSuccess(balance)
    }

    private suspend fun RoutingContext.win(token: SessionToken) {
        val balance = SpinService.settle(
            token = token,
            extRoundId = call.parameters.roundId,
            transactionId = call.parameters.transactionId,
            amount = call.parameters.amount
        ).getOrElse {
            throw OneGameHubError.transform(it)
        }

        call.respondSuccess(balance)
    }

    private suspend fun ApplicationCall.respondSuccess(balance: Balance) {
        val totalAmount = OneGameHubCurrencyAdapter.convertToAggregator(balance.currency, balance.totalAmount)

        respondSuccess(totalAmount, balance.currency)
    }

    private suspend fun ApplicationCall.respondSuccess(balance: Int, currency: Currency) =
        respond(
            HttpStatusCode.OK, mapOf(
                "status" to 200,
                "balance" to balance,
                "currency" to currency.value
            )
        )
}