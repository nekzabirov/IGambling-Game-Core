package app.adapter

import core.model.Balance
import core.value.Currency

interface WalletAdapter {
    fun findBalance(playerId: String): Result<Balance>

    fun placeBet(playerId: String, amount: Int, currency: Currency): Result<Balance>
}