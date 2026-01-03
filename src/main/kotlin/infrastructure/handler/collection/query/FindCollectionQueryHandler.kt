package com.nekgamebling.infrastructure.handler.collection.query

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.collection.query.FindCollectionQuery
import com.nekgamebling.application.port.inbound.collection.query.FindCollectionResponse
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.mapper.toCollection
import infrastructure.persistence.exposed.table.CollectionGameTable
import infrastructure.persistence.exposed.table.CollectionTable
import infrastructure.persistence.exposed.table.GameTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class FindCollectionQueryHandler : QueryHandler<FindCollectionQuery, FindCollectionResponse> {

    override suspend fun handle(query: FindCollectionQuery): Result<FindCollectionResponse> = newSuspendedTransaction {
        // Count expressions
        val gameCount = CollectionGameTable.gameId.count()
        val providerCount = GameTable.providerId.countDistinct()

        // Single query with join and aggregation
        val row = CollectionTable
            .leftJoin(CollectionGameTable, { CollectionTable.id }, { CollectionGameTable.categoryId })
            .leftJoin(GameTable, { CollectionGameTable.gameId }, { GameTable.id })
            .select(
                CollectionTable.columns +
                gameCount +
                providerCount
            )
            .where { CollectionTable.identity eq query.identity }
            .groupBy(*CollectionTable.columns.toTypedArray())
            .firstOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Collection", query.identity))

        Result.success(
            FindCollectionResponse(
                collection = row.toCollection(),
                providerCount = row[providerCount].toInt(),
                gameCount = row[gameCount].toInt()
            )
        )
    }
}
