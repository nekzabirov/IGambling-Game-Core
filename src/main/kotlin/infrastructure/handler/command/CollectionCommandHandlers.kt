package infrastructure.handler.command

import application.port.inbound.CommandHandler
import application.port.inbound.command.*
import domain.collection.model.Collection
import domain.common.error.DuplicateEntityError
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.mapper.toCollection
import infrastructure.persistence.exposed.mapper.toGame
import infrastructure.persistence.exposed.table.CollectionGameTable
import infrastructure.persistence.exposed.table.CollectionTable
import infrastructure.persistence.exposed.table.GameTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * Command handler for adding a new collection.
 */
class AddCollectionCommandHandler : CommandHandler<AddCollectionCommand, Collection> {
    override suspend fun handle(command: AddCollectionCommand): Result<Collection> = newSuspendedTransaction {
        val exists = CollectionTable.selectAll()
            .where { CollectionTable.identity eq command.identity }
            .count() > 0

        if (exists) {
            return@newSuspendedTransaction Result.failure(DuplicateEntityError("Collection", command.identity))
        }

        val collection = Collection(
            id = UUID.randomUUID(),
            identity = command.identity,
            name = command.name
        )

        val id = CollectionTable.insertAndGetId {
            it[identity] = collection.identity
            it[name] = collection.name
            it[active] = collection.active
            it[order] = collection.order
        }

        Result.success(collection.copy(id = id.value))
    }
}

/**
 * Command handler for updating a collection.
 */
class UpdateCollectionCommandHandler : CommandHandler<UpdateCollectionCommand, Collection> {
    override suspend fun handle(command: UpdateCollectionCommand): Result<Collection> = newSuspendedTransaction {
        val existing = CollectionTable.selectAll()
            .where { CollectionTable.identity eq command.identity }
            .singleOrNull()
            ?.toCollection()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Collection", command.identity))

        val updated = existing.copy(
            name = command.name ?: existing.name,
            order = command.order ?: existing.order,
            active = command.active ?: existing.active
        )

        CollectionTable.update({ CollectionTable.id eq existing.id }) {
            it[name] = updated.name
            it[order] = updated.order
            it[active] = updated.active
        }

        Result.success(updated)
    }
}

/**
 * Command handler for adding a game to a collection.
 */
class AddGameToCollectionCommandHandler : CommandHandler<AddGameToCollectionCommand, Unit> {
    override suspend fun handle(command: AddGameToCollectionCommand): Result<Unit> = newSuspendedTransaction {
        val collection = CollectionTable.selectAll()
            .where { CollectionTable.identity eq command.collectionIdentity }
            .singleOrNull()
            ?.toCollection()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Collection", command.collectionIdentity))

        val game = GameTable.selectAll()
            .where { GameTable.identity eq command.gameIdentity }
            .singleOrNull()
            ?.toGame()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Game", command.gameIdentity))

        // Check if already exists
        val exists = CollectionGameTable.selectAll()
            .where { (CollectionGameTable.categoryId eq collection.id) and (CollectionGameTable.gameId eq game.id) }
            .count() > 0

        if (!exists) {
            // Get max order
            val maxOrder = CollectionGameTable
                .select(CollectionGameTable.order.max())
                .where { CollectionGameTable.categoryId eq collection.id }
                .singleOrNull()
                ?.getOrNull(CollectionGameTable.order.max()) ?: 0

            CollectionGameTable.insert {
                it[categoryId] = collection.id
                it[gameId] = game.id
                it[order] = maxOrder + 1
            }
        }

        Result.success(Unit)
    }
}

/**
 * Command handler for removing a game from a collection.
 */
class RemoveGameFromCollectionCommandHandler : CommandHandler<RemoveGameFromCollectionCommand, Unit> {
    override suspend fun handle(command: RemoveGameFromCollectionCommand): Result<Unit> = newSuspendedTransaction {
        val collection = CollectionTable.selectAll()
            .where { CollectionTable.identity eq command.collectionIdentity }
            .singleOrNull()
            ?.toCollection()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Collection", command.collectionIdentity))

        val game = GameTable.selectAll()
            .where { GameTable.identity eq command.gameIdentity }
            .singleOrNull()
            ?.toGame()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Game", command.gameIdentity))

        CollectionGameTable.deleteWhere {
            (CollectionGameTable.categoryId eq collection.id) and (CollectionGameTable.gameId eq game.id)
        }

        Result.success(Unit)
    }
}

/**
 * Command handler for changing game order in a collection.
 */
class ChangeGameOrderInCollectionCommandHandler : CommandHandler<ChangeGameOrderInCollectionCommand, Unit> {
    override suspend fun handle(command: ChangeGameOrderInCollectionCommand): Result<Unit> = newSuspendedTransaction {
        val collection = CollectionTable.selectAll()
            .where { CollectionTable.identity eq command.collectionIdentity }
            .singleOrNull()
            ?.toCollection()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Collection", command.collectionIdentity))

        val game = GameTable.selectAll()
            .where { GameTable.identity eq command.gameIdentity }
            .singleOrNull()
            ?.toGame()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Game", command.gameIdentity))

        val updated = CollectionGameTable.update({
            (CollectionGameTable.categoryId eq collection.id) and (CollectionGameTable.gameId eq game.id)
        }) {
            it[order] = command.newOrder
        }

        if (updated == 0) {
            return@newSuspendedTransaction Result.failure(
                NotFoundError("Game in Collection", "${command.gameIdentity} in ${command.collectionIdentity}")
            )
        }

        Result.success(Unit)
    }
}
