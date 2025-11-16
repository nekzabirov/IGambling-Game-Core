package infrastructure.aggregator.onegamehub.handler

import core.value.Currency
import domain.aggregator.handler.IAggregatorHttpHandler
import infrastructure.adapter.decorator.GameWalletAdapter
import service.GameService
import service.SessionService
import infrastructure.aggregator.onegamehub.handler.error.OneGameHubInvalidateRequest
import infrastructure.aggregator.onegamehub.handler.error.OneGameHubTokenExpired
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.component.KoinComponent

object OneGameHubHandler : IAggregatorHttpHandler, KoinComponent {
    override fun Route.route() = post("/onegamehub") {
        val action = call.parameters["action"] ?: throw OneGameHubInvalidateRequest()
        val sessionToken = call.parameters["extra"] ?: throw OneGameHubInvalidateRequest()

        if (action == "balance") {
            balance(sessionToken)
        }
    }

    private suspend fun RoutingContext.balance(sessionToken: String) {
        val session = SessionService.findByToken(sessionToken).getOrElse {
            throw OneGameHubTokenExpired()
        }

        val game = GameService.findById(session.gameId).getOrElse {
            throw OneGameHubInvalidateRequest()
        }

        val walletAdapter = GameWalletAdapter(game)

        val balance = walletAdapter.findBalance(session.playerId).getOrElse {
            throw OneGameHubTokenExpired()
        }

        call.respondSuccess(balance.totalAmount, balance.currency)
    }

    private suspend fun ApplicationCall.respondSuccess(balance: Int, currency: Currency) =
        respond(HttpStatusCode.OK, mapOf(
        "status" to 200,
        "balance" to balance,
        "currency" to currency.value
    ))
}