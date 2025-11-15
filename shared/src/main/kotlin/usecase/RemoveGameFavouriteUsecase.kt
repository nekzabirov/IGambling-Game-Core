package usecase

import domain.game.table.GameFavouriteTable
import domain.game.table.GameTable
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class RemoveGameFavouriteUsecase {
    suspend operator fun invoke(gameIdentity: String, playerId: String): Result<Unit> = newSuspendedTransaction {
        val gameId = GameTable.select(GameTable.id)
            .where { GameTable.identity eq gameIdentity }
            .singleOrNull()?.get(GameTable.id)
            ?: return@newSuspendedTransaction Result.failure(NotFoundException("Game not found"))

        GameFavouriteTable
            .deleteWhere { GameFavouriteTable.gameId eq gameId.value and (GameFavouriteTable.playerId eq playerId) }

        Result.success(Unit)
    }
}
