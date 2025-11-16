package app.service

import app.adapter.WalletAdapter
import core.error.InsufficientBalanceError
import core.model.Balance
import domain.game.table.GameTable
import domain.session.model.Session
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.component.KoinComponent

object BetService : KoinComponent {
    val walletAdapter = getKoin().get<WalletAdapter>()

    suspend fun findBalance(session: Session): Result<Balance> = newSuspendedTransaction {
        val balance = walletAdapter.findBalance(session.playerId).getOrElse {
            return@newSuspendedTransaction Result.failure(it)
        }

        val isBonusBetAllow = GameTable.select(GameTable.id, GameTable.bonusBetEnable)
            .where { GameTable.id eq session.gameId }
            .singleOrNull()?.get(GameTable.bonusBetEnable) ?: false

        Result.success(balance.copy(bonus = if (isBonusBetAllow) balance.bonus else 0))
    }

    suspend fun placeBet(session: Session, amount: Int): Result<Balance> = newSuspendedTransaction {
        val balance = findBalance(session).getOrElse {
            return@newSuspendedTransaction Result.failure(it)
        }

        if (amount > balance.totalAmount) {
            throw InsufficientBalanceError()
        }

        val realAmount = if (balance.real < amount) balance.real else amount
        val bonusAmount = if (realAmount == amount) 0 else amount - realAmount

        walletAdapter.withdraw(session.playerId, session.currency, realAmount, bonusAmount)
            .getOrElse { return@newSuspendedTransaction Result.failure(it) }

        Result.success(
            balance.copy(
                real = balance.real - realAmount,
                bonus = balance.bonus - bonusAmount
            )
        )
    }
}