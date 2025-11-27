package app.usecase

import app.service.GameService
import core.error.GameUnavailableError
import core.error.SessionUnavailingError
import core.model.Balance
import core.value.SessionToken
import domain.game.dao.findBySymbol
import domain.game.mapper.toGame
import domain.game.table.GameTable
import domain.session.dao.findByToken
import domain.session.table.SessionTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.component.KoinComponent
import kotlin.getOrElse

class FindPlayerGameBalance : KoinComponent {
    val walletAdapter = getKoin().get<app.adapter.WalletAdapter>()

    suspend operator fun invoke(token: SessionToken, gameSymbol: String? = null): Result<Balance> {
        val (session, game) = newSuspendedTransaction {
            val session = SessionTable.findByToken(token.value) ?: return@newSuspendedTransaction Result.failure(
                SessionUnavailingError()
            )

            val game = if (gameSymbol != null) {
                GameService.findBySymbol(gameSymbol)
                    .getOrElse { return@newSuspendedTransaction Result.failure(it)}
            } else {
                GameTable.selectAll()
                    .where { GameTable.id eq session.gameId }
                    .single().toGame()
            }

            Result.success(session to game)
        }.getOrElse { return Result.failure(it) }

        return walletAdapter.findBalance(session.playerId)
            .map {
                if (!game.bonusBetEnable)
                    it.copy(bonus = 0)
                else it
            }
    }
}