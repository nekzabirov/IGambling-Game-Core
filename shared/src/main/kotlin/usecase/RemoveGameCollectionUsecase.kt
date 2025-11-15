package usecase

import domain.collection.table.CollectionGameTable
import domain.collection.table.CollectionTable
import domain.game.table.GameTable
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class RemoveGameCollectionUsecase {
    suspend operator fun invoke(identity: String, gameIdentity: String): Result<Unit> = newSuspendedTransaction {
        val collectionId = CollectionTable.select(CollectionTable.id)
            .where { CollectionTable.identity eq identity }
            .singleOrNull()?.get(CollectionTable.id)
            ?: return@newSuspendedTransaction Result.failure(NotFoundException("Collection not found"))

        val gameId = GameTable.select(GameTable.id)
            .where { GameTable.identity eq gameIdentity }
            .singleOrNull()?.get(GameTable.id)
            ?: return@newSuspendedTransaction Result.failure(NotFoundException("Game not found"))

        CollectionGameTable
            .deleteWhere { CollectionGameTable.categoryId eq collectionId.value and (CollectionGameTable.gameId eq gameId.value) }

        Result.success(Unit)
    }
}