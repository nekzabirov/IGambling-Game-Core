package infrastructure.adapter

import application.port.outbound.WalletAdapter
import domain.session.model.Balance
import shared.value.Currency
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap

/**
 * Fake wallet adapter for development/testing.
 * Replace with real implementation in production.
 */
class FakeWalletAdapter : WalletAdapter {
    companion object {
        private var realAmount = 10000.toBigInteger()
        private var bonusAmount = 1000.toBigInteger()
        private val currency = Currency("EUR")
    }

    override suspend fun findBalance(playerId: String): Result<Balance> {
        return Balance(realAmount, bonusAmount, currency).let { Result.success(it) }
    }

    override suspend fun withdraw(
        playerId: String,
        transactionId: String,
        currency: Currency,
        realAmount: BigInteger,
        bonusAmount: BigInteger
    ): Result<Unit> {
        FakeWalletAdapter.realAmount -= realAmount
        FakeWalletAdapter.bonusAmount -= bonusAmount
        return Result.success(Unit)
    }

    override suspend fun deposit(
        playerId: String,
        transactionId: String,
        currency: Currency,
        realAmount: BigInteger,
        bonusAmount: BigInteger
    ): Result<Unit> {
        FakeWalletAdapter.realAmount += realAmount
        FakeWalletAdapter.bonusAmount += bonusAmount
        return Result.success(Unit)
    }

    override suspend fun rollback(playerId: String, transactionId: String): Result<Unit> {
        return Result.success(Unit)
    }

}
