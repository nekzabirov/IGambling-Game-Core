package app.usecase

import app.adapter.EventProducerAdapter
import app.event.GameWonEvent
import core.value.Currency
import domain.game.dao.full
import domain.game.mapper.toGameFull
import domain.game.table.GameTable
import domain.game.table.GameWonTable
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.component.KoinComponent

class AddGameWonUsecase : KoinComponent {
    private val eventProducer = getKoin().get<EventProducerAdapter>()

    suspend operator fun invoke(gameIdentity: String, playerId: String, amount: Int, currency: Currency): Result<Unit> = newSuspendedTransaction {
        val game = GameTable.full()
            .andWhere { GameTable.identity eq gameIdentity }
            .singleOrNull()?.toGameFull() ?: return@newSuspendedTransaction Result.failure(NotFoundException("Game not found"))

        GameWonTable.insert {
            it[GameWonTable.gameId] = game.id
            it[GameWonTable.playerId] = playerId
            it[GameWonTable.amount] = amount
            it[GameWonTable.currency] = currency.value
        }

        eventProducer.publish(GameWonEvent(
            game = game,
            playerId = playerId,
            amount = amount,
            currency = currency
        ))

        Result.success(Unit)
    }
}
