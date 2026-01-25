package com.nekgamebling.infrastructure.handler.collection.command

import application.port.inbound.CommandHandler
import com.nekgamebling.application.port.inbound.collection.command.CreateCollectionCommand
import com.nekgamebling.application.port.inbound.collection.command.CreateCollectionResponse
import domain.collection.model.Collection
import domain.common.error.DuplicateEntityError
import infrastructure.persistence.exposed.table.CollectionTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class CreateCollectionCommandHandler : CommandHandler<CreateCollectionCommand, CreateCollectionResponse> {

    override suspend fun handle(command: CreateCollectionCommand): Result<CreateCollectionResponse> = newSuspendedTransaction {
        // Check if collection with same identity already exists
        val exists = CollectionTable
            .selectAll()
            .where { CollectionTable.identity eq command.identity }
            .count() > 0

        if (exists) {
            return@newSuspendedTransaction Result.failure(
                DuplicateEntityError("Collection", command.identity)
            )
        }

        val collection = Collection(
            id = UUID.randomUUID(),
            identity = command.identity,
            name = command.name,
            active = command.active,
            order = command.order
        )

        CollectionTable.insert {
            it[id] = collection.id
            it[identity] = collection.identity
            it[name] = collection.name
            it[active] = collection.active
            it[order] = collection.order
        }

        Result.success(CreateCollectionResponse(collection))
    }
}
