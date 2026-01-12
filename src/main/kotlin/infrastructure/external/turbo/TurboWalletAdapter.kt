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

class TurboWalletAdapter : WalletAdapter {

    private val client = TurboHttpClient.client

    private val urlAddress by lazy {
        System.getenv()["TURBO_WALLET_URL"] ?: "http://localhost:8080"
    }

    override suspend fun findBalance(playerId: String): Result<Balance> = runCatching {
        // Check cache first (saves ~200ms HTTP call)
        BalanceCache.get(playerId)?.let { cached ->
            Logger.info("[CACHE HIT] balance for player=$playerId")
            return@runCatching cached
        }

        val walletResponse: TurboResponse<List<AccountDto>> = Logger.profileSuspend("turbo.wallet.findBalance") {
            client.get("$urlAddress/accounts/find") {
                parameter("playerId", playerId)
            }.body()
        }

        if (walletResponse.data == null) throw Exception("Failed to fetch balance from TurboWallet")

        val account = walletResponse.data.firstOrNull { it.status == 1 } ?: throw Exception("Failed to fetch balance from TurboWallet")

        val balance = Balance(
            real = account.realBalance + account.lockedBalance,
            bonus = account.bonusBalance,
            currency = Currency(account.currency)
        )

        // Cache for subsequent requests
        BalanceCache.put(playerId, balance)

        return@runCatching balance
    }

    override suspend fun withdraw(
        playerId: String,
        transactionId: String,
        currency: Currency,
        realAmount: Long,
        bonusAmount: Long
    ): Result<Balance> = runCatching {
        val request = BetTransactionRequest(
            playerId = playerId,
            amount = realAmount + bonusAmount,
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

        val balance = Balance(
            real = tx.realBalance + tx.lockedBalance,
            bonus = tx.bonusBalance,
            currency = Currency(tx.currency)
        )

        // Update cache with actual balance from wallet
        BalanceCache.put(playerId, balance)

        balance
    }

    override suspend fun deposit(
        playerId: String,
        transactionId: String,
        currency: Currency,
        realAmount: Long,
        bonusAmount: Long
    ): Result<Balance> = runCatching {
        // Zero-amount check is now handled at saga step level
        val request = SettleTransactionRequest(
            playerId = playerId,
            amount = realAmount + bonusAmount,
            currency = currency.value,
            externalId = transactionId,
            referencedExternalId = transactionId,
            balanceType = if (bonusAmount > 0L) BalanceType.BONUS else BalanceType.REAL
        )

        val response: TurboResponse<List<TransactionResponseDto>> = client.post("$urlAddress/bets/settle") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        val tx = response.data?.firstOrNull()
            ?: throw Exception("No transaction data in deposit response")

        val balance = Balance(
            real = tx.realBalance + tx.lockedBalance,
            bonus = tx.bonusBalance,
            currency = Currency(tx.currency)
        )

        // Update cache with actual balance from wallet
        BalanceCache.put(playerId, balance)

        balance
    }

    override suspend fun rollback(playerId: String, transactionId: String): Result<Unit> {
        return Result.success(Unit)
    }
}
