package com.nekgamebling.infrastructure.handler.collection.command

import application.port.inbound.CommandHandler
import com.nekgamebling.application.port.inbound.collection.command.UpdateCollectionCommand
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.table.CollectionTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class UpdateCollectionCommandHandler : CommandHandler<UpdateCollectionCommand, Unit> {

    override suspend fun handle(command: UpdateCollectionCommand): Result<Unit> = newSuspendedTransaction {
        val exists = CollectionTable
            .selectAll()
            .where { CollectionTable.identity eq command.identity }
            .count() > 0

        if (!exists) {
            return@newSuspendedTransaction Result.failure(NotFoundError("Collection", command.identity))
        }

        // Only update if there's something to update
        if (command.active != null || command.order != null) {
            CollectionTable.update({ CollectionTable.identity eq command.identity }) {
                command.active?.let { value -> it[active] = value }
                command.order?.let { value -> it[order] = value }
            }
        }

        Result.success(Unit)
    }
}
