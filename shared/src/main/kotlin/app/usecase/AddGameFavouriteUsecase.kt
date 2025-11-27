package app.usecase

import app.adapter.EventProducerAdapter
import app.event.GameFavouriteEvent
import domain.game.dao.full
import domain.game.mapper.toGameFull
import domain.game.table.GameFavouriteTable
import domain.game.table.GameTable
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.component.KoinComponent

class AddGameFavouriteUsecase : KoinComponent {
    private val eventProducer = getKoin().get<EventProducerAdapter>()

    suspend operator fun invoke(gameIdentity: String, playerId: String): Result<Unit> = newSuspendedTransaction {
        val game = GameTable.full()
            .andWhere { GameTable.identity eq gameIdentity }
            .singleOrNull()?.toGameFull() ?: return@newSuspendedTransaction Result.failure(NotFoundException("Game not found"))

        GameFavouriteTable
            .select(GameFavouriteTable.gameId)
            .where { GameFavouriteTable.gameId eq game.id and (GameFavouriteTable.playerId eq playerId) }
            .count()
            .also {
                if (it > 0) {
                    return@newSuspendedTransaction Result.success(Unit)
                }
            }

        GameFavouriteTable.insert {
            it[GameFavouriteTable.gameId] = game.id
            it[GameFavouriteTable.playerId] = playerId
        }

        eventProducer.publish(GameFavouriteEvent(
            game = game,
            playerId = playerId
        ))

        Result.success(Unit)
    }
}
