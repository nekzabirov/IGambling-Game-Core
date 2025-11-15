package usecase

import domain.game.table.GameFavouriteTable
import domain.game.table.GameTable
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class AddGameFavouriteUsecase {
    suspend operator fun invoke(gameIdentity: String, playerId: String): Result<Unit> = newSuspendedTransaction {
        val gameId = GameTable.select(GameTable.id)
            .where { GameTable.identity eq gameIdentity }
            .singleOrNull()?.get(GameTable.id)
            ?: return@newSuspendedTransaction Result.failure(NotFoundException("Game not found"))

        GameFavouriteTable
            .select(GameFavouriteTable.gameId)
            .where { GameFavouriteTable.gameId eq gameId.value and (GameFavouriteTable.playerId eq playerId) }
            .count()
            .also {
                if (it > 0) {
                    return@newSuspendedTransaction Result.success(Unit)
                }
            }

        GameFavouriteTable.insert {
            it[GameFavouriteTable.gameId] = gameId.value
            it[GameFavouriteTable.playerId] = playerId
        }

        Result.success(Unit)
    }
}
