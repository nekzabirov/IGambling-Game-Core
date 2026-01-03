package infrastructure.external.turbo

import application.port.outbound.WalletAdapter
import infrastructure.external.turbo.dto.AccountDto
import infrastructure.external.turbo.dto.BalanceType
import infrastructure.external.turbo.dto.BetTransactionRequest
import infrastructure.external.turbo.dto.SettleTransactionRequest
import infrastructure.external.turbo.dto.TransactionResponseDto
import infrastructure.external.turbo.dto.TurboResponse
import domain.session.model.Balance
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import shared.Logger
import shared.value.Currency
import java.math.BigInteger

class TurboWalletAdapter : WalletAdapter {

    private val client = TurboHttpClient.client

    private val urlAddress by lazy {
        System.getenv()["TURBO_WALLET_URL"] ?: "http://localhost:8080"
    }

    override suspend fun findBalance(playerId: String): Result<Balance> = runCatching {
        val walletResponse: TurboResponse<List<AccountDto>> = Logger.profileSuspend("turbo.wallet.finBalance") {
            client.get("$urlAddress/accounts/find") {
                parameter("playerId", playerId)
            }.body()
        }

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
    ): Result<Balance> = runCatching {
        val request = BetTransactionRequest(
            playerId = playerId,
            amount = (realAmount + bonusAmount).toLong(),
            currency = currency.value,
            externalId = transactionId,
            balanceTypeOrder = listOf(BalanceType.REAL, BalanceType.LOCKED, BalanceType.BONUS)
        )

        val response: TurboResponse<List<TransactionResponseDto>> = Logger.profileSuspend("turbo.wallet.withdraw") {
            client.post("$urlAddress/bets/placebet") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }

        val tx = response.data?.firstOrNull()
            ?: throw Exception("No transaction data in withdraw response")

        Balance(
            real = tx.realBalance.toBigInteger() + tx.lockedBalance.toBigInteger(),
            bonus = tx.bonusBalance.toBigInteger(),
            currency = Currency(tx.currency)
        )
    }

    override suspend fun deposit(
        playerId: String,
        transactionId: String,
        currency: Currency,
        realAmount: BigInteger,
        bonusAmount: BigInteger
    ): Result<Balance> = runCatching {
        if (realAmount + bonusAmount <= BigInteger.ZERO) {
            // No deposit needed, fetch current balance
            return findBalance(playerId)
        }

        val request = SettleTransactionRequest(
            playerId = playerId,
            amount = (realAmount + bonusAmount).toLong(),
            currency = currency.value,
            externalId = transactionId,
            referencedExternalId = transactionId,
            balanceType = if (bonusAmount > BigInteger.ZERO) BalanceType.BONUS else BalanceType.REAL
        )

        val response: TurboResponse<List<TransactionResponseDto>> = client.post("$urlAddress/bets/settle") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        val tx = response.data?.firstOrNull()
            ?: throw Exception("No transaction data in deposit response")

        Balance(
            real = tx.realBalance.toBigInteger() + tx.lockedBalance.toBigInteger(),
            bonus = tx.bonusBalance.toBigInteger(),
            currency = Currency(tx.currency)
        )
    }

    override suspend fun rollback(playerId: String, transactionId: String): Result<Unit> {
        return Result.success(Unit)
    }
}
