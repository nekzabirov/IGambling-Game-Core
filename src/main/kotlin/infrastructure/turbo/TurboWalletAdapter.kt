package com.nekgamebling.infrastructure.turbo

import application.port.outbound.WalletAdapter
import com.nekgamebling.infrastructure.turbo.dto.AccountDto
import com.nekgamebling.infrastructure.turbo.dto.BalanceType
import com.nekgamebling.infrastructure.turbo.dto.BetTransactionRequest
import com.nekgamebling.infrastructure.turbo.dto.SettleTransactionRequest
import com.nekgamebling.infrastructure.turbo.dto.TurboResponse
import domain.session.model.Balance
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import shared.value.Currency
import java.math.BigInteger

class TurboWalletAdapter : WalletAdapter {

    private val client = TurboHttpClient.client

    private val urlAddress by lazy {
        System.getenv()["TURBO_WALLET_URL"] ?: "http://localhost:8080"
    }

    override suspend fun findBalance(playerId: String): Result<Balance> = runCatching {
        val walletResponse: TurboResponse<List<AccountDto>> = client.get("$urlAddress/accounts/find") {
            parameter("playerId", playerId)
        }.body()

        if (walletResponse.data == null) throw Exception("Failed to fetch balance from TurboWallet")

        val account = walletResponse.data.firstOrNull { it.status == 1 } ?: throw Exception("Failed to fetch balance from TurboWallet")

        return@runCatching Balance(
            real = account.realBalance.toBigInteger() + account.lockedBalance.toBigInteger(),
            bonus = account.bonusBalance.toBigInteger(),
            currency = Currency(account.currency)
        )
    }

    override suspend fun withdraw(
        playerId: String,
        transactionId: String,
        currency: Currency,
        realAmount: BigInteger,
        bonusAmount: BigInteger
    ): Result<Unit> = runCatching {
        val request = BetTransactionRequest(
            playerId = playerId,
            amount = (realAmount + bonusAmount).toLong(),
            currency = currency.value,
            externalId = transactionId,
            balanceTypeOrder = listOf(BalanceType.REAL, BalanceType.LOCKED, BalanceType.BONUS)
        )

        client.post("$urlAddress/bets/placebet") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyAsText()

        return Result.success(Unit)
    }

    override suspend fun deposit(
        playerId: String,
        transactionId: String,
        currency: Currency,
        realAmount: BigInteger,
        bonusAmount: BigInteger
    ): Result<Unit> = runCatching {
        if (realAmount + bonusAmount <= BigInteger.ZERO) return Result.success(Unit)

        val request = SettleTransactionRequest(
            playerId = playerId,
            amount = (realAmount + bonusAmount).toLong(),
            currency = currency.value,
            externalId = transactionId,
            referencedExternalId = transactionId,
            balanceType = if (bonusAmount > BigInteger.ZERO) BalanceType.BONUS else BalanceType.REAL
        )

        println("Send deposit request: $request")

        val body = client.post("$urlAddress/bets/settle") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyAsText()

        println("Deposit response: $body")

        return Result.success(Unit)
    }

    override suspend fun rollback(playerId: String, transactionId: String): Result<Unit> {
        return Result.success(Unit)
    }
}
