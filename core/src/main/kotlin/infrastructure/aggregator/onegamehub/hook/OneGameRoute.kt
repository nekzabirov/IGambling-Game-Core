package infrastructure.aggregator.onegamehub.hook

import app.usecase.CloseRoundUsecase
import app.usecase.FindPlayerGameBalance
import app.usecase.PlaceSpinUsecase
import app.usecase.SettleSpinUsecase
import core.model.Balance
import core.value.SessionToken
import infrastructure.aggregator.onegamehub.adapter.OneGameHubCurrencyAdapter
import infrastructure.aggregator.onegamehub.adapter.OneGameHubCurrencyAdapter.getKoin
import infrastructure.aggregator.onegamehub.hook.error.OneGameHubError
import infrastructure.aggregator.onegamehub.hook.error.OneGameHubInvalidateRequest
import infrastructure.aggregator.onegamehub.hook.error.OneGameHubTokenExpired
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val Parameters.amount get() = this["amount"]!!.toInt()
private val Parameters.gameSymbol get() = this["game_id"]!!
private val Parameters.transactionId get() = this["transaction_id"]!!
private val Parameters.roundId get() = this["round_id"]!!
private val Parameters.freespinId get() = this["freerounds_id"]
private val Parameters.isRoundEnd get() = this["ext_round_finished"] == "1"

internal fun Route.oneGameHubRoute() = post("/onegamehub") {
    val action = call.queryParameters["action"]
    val sessionToken = call.queryParameters["extra"]

    if (action == null || sessionToken == null) {
        call.respondFail(OneGameHubInvalidateRequest())
        return@post
    }

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
            call.respondFail(OneGameHubInvalidateRequest())
        }
    }
}

private suspend fun RoutingContext.balance(token: SessionToken) {
    val findBalance = getKoin().get<FindPlayerGameBalance>()

    val balance = findBalance(token).getOrElse {
        call.respondFail(OneGameHubTokenExpired())
        return
    }

    call.respondSuccess(balance)
}

private suspend fun RoutingContext.bet(token: SessionToken) {
    val placeSpinUsecase = getKoin().get<PlaceSpinUsecase>()
    val findBalance = getKoin().get<FindPlayerGameBalance>()

    placeSpinUsecase(
        token = token,
        gameSymbol = call.queryParameters.gameSymbol,
        extRoundId = call.queryParameters.roundId,
        exTransactionId = call.queryParameters.transactionId,
        freeSpinId = call.queryParameters.freespinId,
        amount = call.queryParameters.amount,
    ).getOrElse {
        call.respond(OneGameHubError.transform(it))
        return
    }

    val balance = findBalance(token).getOrElse {
        call.respondFail(OneGameHubTokenExpired())
        return
    }

    call.respondSuccess(balance)
}

private suspend fun RoutingContext.win(token: SessionToken) {
    val findBalance = getKoin().get<FindPlayerGameBalance>()
    val settleSpinUsecase = getKoin().get<SettleSpinUsecase>()
    val closeRound = getKoin().get<CloseRoundUsecase>()

    if (call.queryParameters.isRoundEnd && call.queryParameters.amount <= 0) {
        closeRound(
            token,
            extRoundId = call.queryParameters.roundId,
            freeSpinId = call.queryParameters.freespinId
        ).getOrElse {
            call.respond(OneGameHubError.transform(it))
            return
        }
    } else {
        settleSpinUsecase(
            token,
            extRoundId = call.queryParameters.roundId,
            exTransactionId = call.queryParameters.transactionId,
            freeSpinId = call.queryParameters.freespinId,
            amount = call.queryParameters.amount
        ).getOrElse {
            call.respond(OneGameHubError.transform(it))
            return
        }
    }

    val balance = findBalance(token).getOrElse {
        call.respondFail(OneGameHubTokenExpired())
        return
    }

    call.respondSuccess(balance)
}

private suspend fun ApplicationCall.respondSuccess(balance: Balance) {
    val totalAmount = OneGameHubCurrencyAdapter.convertToAggregator(balance.currency, balance.totalAmount)

    respond(OneGameHubBalanceDto(balance = totalAmount, currency = balance.currency.value))
}

private suspend fun ApplicationCall.respondFail(error: OneGameHubError) {
    respond(HttpStatusCode.BadRequest, error.body)
}