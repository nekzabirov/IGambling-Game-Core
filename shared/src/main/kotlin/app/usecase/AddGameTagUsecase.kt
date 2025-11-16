package app.usecase

import domain.game.table.GameTable
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class AddGameTagUsecase {
    suspend operator fun invoke(identity: String, tag: String): Result<Unit> = newSuspendedTransaction {
        val game = GameTable.select(GameTable.id, GameTable.tags)
            .where { GameTable.identity eq identity }
            .singleOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundException("Game not found"))

        val currentTags = game[GameTable.tags]

        if (currentTags.contains(tag)) {
            return@newSuspendedTransaction Result.success(Unit)
        }

        val updatedTags = currentTags + tag

        GameTable.update({ GameTable.identity eq identity }) {
            it[GameTable.tags] = updatedTags
        }

        Result.success(Unit)
    }
}
