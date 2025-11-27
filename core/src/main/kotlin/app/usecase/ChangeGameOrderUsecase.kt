package app.usecase

import domain.collection.table.CollectionGameTable
import domain.collection.table.CollectionTable
import domain.game.table.GameTable
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class ChangeGameOrderUsecase {
    suspend operator fun invoke(collectionIdentity: String, gameIdentity: String, order: Int): Result<Unit> =
        newSuspendedTransaction {
            val collectionId = CollectionTable.select(CollectionTable.id)
                .where { CollectionTable.identity eq collectionIdentity }
                .singleOrNull()?.get(CollectionTable.id)
                ?: return@newSuspendedTransaction Result.failure(NotFoundException("Collection not found"))

            val gameId = GameTable.select(GameTable.id)
                .where { GameTable.identity eq gameIdentity }
                .singleOrNull()?.get(GameTable.id)
                ?: return@newSuspendedTransaction Result.failure(NotFoundException("Game not found"))

            CollectionGameTable
                .update(where = { CollectionGameTable.categoryId eq collectionId.value and (CollectionGameTable.gameId eq gameId.value) }) {
                    it[CollectionGameTable.order] = order
                }

            Result.success(Unit)
        }
}