package com.nekgamebling.infrastructure.aggregator.pragmatic.handler

import com.nekgamebling.infrastructure.aggregator.pragmatic.handler.dto.PragmaticBetPayload
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import shared.value.SessionToken

private val RoutingCall.sessionToken
    get() = parameters["sessionToken"]

private val RoutingCall.gameId
    get() = parameters["gameId"]

private val RoutingCall.reference
    get() = parameters["reference"]

private val RoutingCall.roundId
    get() = parameters["roundId"]

private val RoutingCall.bonusCode
    get() = parameters["bonusCode"]

private val RoutingCall.amount
    get() = parameters["amount"]


internal fun Route.pragmaticRoute() {
    val handler: PragmaticHandler by application.inject<PragmaticHandler>()

    route("/pragmatic") {
        authenticate(handler)
        balance(handler)
        bet(handler)
        result(handler)
        bonusWin(handler)
        jackpotWin(handler)
        refund(handler)
        endRound(handler)
        adjustment(handler)
    }
}

private fun Route.authenticate(handler: PragmaticHandler) = get("authenticate.html") {
    val sessionToken = SessionToken(call.sessionToken!!)

    call.respond(handler.authenticate(sessionToken))
}

private fun Route.balance(handler: PragmaticHandler) = get("balance.html") {
    val sessionToken = SessionToken(call.sessionToken!!)

    call.respond(handler.balance(sessionToken))
}

private fun Route.bet(handler: PragmaticHandler) = get("bet.html") {
    val sessionToken = SessionToken(call.sessionToken!!)

    val payload = PragmaticBetPayload(
        reference = call.reference!!,
        gameId = call.gameId!!,
        roundId = call.roundId!!,
        bonusCode = call.bonusCode,
        amount = call.amount!!
    )

    call.respond(handler.bet(sessionToken, payload))
}

private fun Route.result(handler: PragmaticHandler) = get("result.html") {
    val sessionToken = SessionToken(call.sessionToken!!)

    val payload = PragmaticBetPayload(
        reference = call.reference!!,
        gameId = call.gameId!!,
        roundId = call.roundId!!,
        bonusCode = call.bonusCode,
        amount = call.amount!!,
        promoWinAmount = call.parameters["promoWinAmount"] ?: "0"
    )

    call.respond(handler.result(sessionToken, payload))
}

/**
 * Used to inform freespin enede and got total win amount
 */
private fun Route.bonusWin(handler: PragmaticHandler) = get("bonusWin.html") {
    val sessionToken = SessionToken(call.sessionToken!!)

    call.respond(handler.balance(sessionToken))
}

private fun Route.jackpotWin(handler: PragmaticHandler) = get("jackpotWin.html") {
    val sessionToken = SessionToken(call.sessionToken!!)

    //TODO: Use for feature in jackpot win use case

    call.respond(handler.balance(sessionToken))
}

private fun Route.refund(handler: PragmaticHandler) = get("refund.html") {

}

private fun Route.endRound(handler: PragmaticHandler) = get("endRound.html") {
    val sessionToken = SessionToken(call.sessionToken!!)

    call.respond(handler.endRound(sessionToken, call.roundId!!))
}

private fun Route.adjustment(handler: PragmaticHandler) = get("adjustment.html") {
    val sessionToken = SessionToken(call.sessionToken!!)

    val roundId = call.roundId!!
    val reference = call.reference!!
    val amount = call.amount!!

    call.respond(handler.adjustment(sessionToken, roundId, reference, amount))
}