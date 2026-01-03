package application.port.outbound

import domain.session.model.Balance
import shared.value.Currency
import java.math.BigInteger

/**
 * Port interface for wallet operations.
 * Implementations connect to external wallet service.
 */
interface WalletAdapter {
    /**
     * Get player's current balance.
     */
    suspend fun findBalance(playerId: String): Result<Balance>

    /**
     * Withdraw funds from player's wallet.
     * Returns the updated balance after withdrawal.
     */
    suspend fun withdraw(
        playerId: String,
        transactionId: String,
        currency: Currency,
        realAmount: BigInteger,
        bonusAmount: BigInteger
    ): Result<Balance>

    /**
     * Deposit funds to player's wallet.
     * Returns the updated balance after deposit.
     */
    suspend fun deposit(
        playerId: String,
        transactionId: String,
        currency: Currency,
        realAmount: BigInteger,
        bonusAmount: BigInteger
    ): Result<Balance>

    /**
     * Rollback a previous transaction.
     */
    suspend fun rollback(playerId: String, transactionId: String): Result<Unit>
}
