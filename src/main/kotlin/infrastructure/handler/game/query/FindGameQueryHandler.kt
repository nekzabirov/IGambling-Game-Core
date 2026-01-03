package com.nekgamebling.infrastructure.handler.game.query

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.game.query.FindGameQuery
import com.nekgamebling.application.port.inbound.game.query.FindGameResponse
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.mapper.*
import infrastructure.persistence.exposed.table.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class FindGameQueryHandler : QueryHandler<FindGameQuery, FindGameResponse> {
    override suspend fun handle(query: FindGameQuery): Result<FindGameResponse> = newSuspendedTransaction {
        val row = GameTable
            .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
            .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
            .innerJoin(GameVariantTable, { GameVariantTable.gameId }, { GameTable.id }) {
                GameVariantTable.aggregator eq AggregatorInfoTable.aggregator
            }
            .selectAll()
            .where { GameTable.identity eq query.identity }
            .firstOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Game", query.identity))

        val game = row.toGame()

        val collections = CollectionTable
            .join(CollectionGameTable, JoinType.INNER, CollectionTable.id, CollectionGameTable.categoryId)
            .selectAll()
            .where { CollectionGameTable.gameId eq game.id }
            .map { it.toCollection() }

        Result.success(
            FindGameResponse(
                game = game,
                provider = row.toProvider(),
                activeVariant = row.toGameVariant(),
                aggregator = row.toAggregatorInfo(),
                collections = collections
            )
        )
    }
}