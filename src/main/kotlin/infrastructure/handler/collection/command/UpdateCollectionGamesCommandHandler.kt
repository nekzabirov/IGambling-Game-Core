package com.nekgamebling.infrastructure.handler.collection.command

import application.port.inbound.CommandHandler
import com.nekgamebling.application.port.inbound.collection.command.UpdateCollectionGamesCommand
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.table.CollectionGameTable
import infrastructure.persistence.exposed.table.CollectionTable
import infrastructure.persistence.exposed.table.GameTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class UpdateCollectionGamesCommandHandler : CommandHandler<UpdateCollectionGamesCommand, Unit> {

    override suspend fun handle(command: UpdateCollectionGamesCommand): Result<Unit> = newSuspendedTransaction {
        // Find collection
        val collection = CollectionTable
            .selectAll()
            .where { CollectionTable.identity eq command.identity }
            .firstOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Collection", command.identity))

        val collectionId = collection[CollectionTable.id]

        // Remove games
        if (command.removeGames.isNotEmpty()) {
            // Get game IDs for the identities to remove
            val gameIdsToRemove = GameTable
                .select(GameTable.id)
                .where { GameTable.identity inList command.removeGames }
                .map { it[GameTable.id] }

            if (gameIdsToRemove.isNotEmpty()) {
                CollectionGameTable.deleteWhere {
                    (CollectionGameTable.categoryId eq collectionId) and
                    (CollectionGameTable.gameId inList gameIdsToRemove)
                }
            }
        }

        // Add games
        if (command.addGames.isNotEmpty()) {
            // Get game IDs for the identities to add
            val gamesToAdd = GameTable
                .select(GameTable.id)
                .where { GameTable.identity inList command.addGames }
                .map { it[GameTable.id] }

            // Get existing game IDs in this collection
            val existingGameIds = CollectionGameTable
                .select(CollectionGameTable.gameId)
                .where { CollectionGameTable.categoryId eq collectionId }
                .map { it[CollectionGameTable.gameId] }
                .toSet()

            // Insert only games that don't exist yet
            val newGames = gamesToAdd.filter { it !in existingGameIds }
            if (newGames.isNotEmpty()) {
                CollectionGameTable.batchInsert(newGames) { gameId ->
                    this[CollectionGameTable.categoryId] = collectionId
                    this[CollectionGameTable.gameId] = gameId
                    this[CollectionGameTable.order] = 0
                }
            }
        }

        Result.success(Unit)
    }
}
