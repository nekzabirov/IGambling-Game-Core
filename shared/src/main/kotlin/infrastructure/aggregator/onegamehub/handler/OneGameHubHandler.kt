package infrastructure.aggregator.onegamehub.handler

import app.service.BetService
import app.service.GameService
import app.service.SessionService
import core.model.Balance
import core.value.Currency
import domain.aggregator.handler.IAggregatorHttpHandler
import domain.session.model.Session
import infrastructure.aggregator.onegamehub.adapter.OneGameHubCurrencyAdapter
import infrastructure.aggregator.onegamehub.handler.error.OneGameHubError
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

        val session = SessionService.findByToken(sessionToken).getOrElse {
            throw OneGameHubTokenExpired()
        }

        when (action) {
            "balance" -> {
                balance(session)
            }
            "bet" -> {
                bet(session)
            }
            else -> {
                throw OneGameHubInvalidateRequest()
            }
        }
    }

    private suspend fun RoutingContext.balance(session: Session) {
        val balance = BetService.findBalance(session).getOrElse {
            throw OneGameHubTokenExpired()
        }

        call.respondSuccess(balance)
    }

    private suspend fun RoutingContext.bet(session: Session) {
        val betAmount = call.parameters["betAmount"]?.toIntOrNull() ?: throw OneGameHubInvalidateRequest()

        val systemBetAmount = OneGameHubCurrencyAdapter.convertFromAggregator(session.currency, betAmount)

        val balance = BetService.placeBet(session, systemBetAmount).getOrElse {
            throw OneGameHubError.transform(it)
        }

        call.respondSuccess(balance)
    }

    private suspend fun ApplicationCall.respondSuccess(balance: Balance) {
        val totalAmount = OneGameHubCurrencyAdapter.convertToAggregator(balance.currency, balance.totalAmount)

        respond(HttpStatusCode.OK, mapOf(
            "status" to 200,
            "balance" to totalAmount,
            "currency" to balance.currency.value
        ))
    }

    private suspend fun ApplicationCall.respondSuccess(balance: Int, currency: Currency) =
        respond(HttpStatusCode.OK, mapOf(
        "status" to 200,
        "balance" to balance,
        "currency" to currency.value
    ))
}