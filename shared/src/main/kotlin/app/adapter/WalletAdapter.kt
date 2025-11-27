package app.adapter

import core.model.Balance
import core.value.Currency

interface WalletAdapter {
    suspend fun findBalance(playerId: String): Result<Balance>

    suspend fun withdraw(playerId: String, referenceId: String, currency: Currency, real: Int, bonus: Int): Result<Unit>

    suspend fun deposit(playerId: String, referenceId: String, currency: Currency, real: Int, bonus: Int): Result<Unit>

    suspend fun rollback(playerId: String, referenceId: String): Result<Unit>
}