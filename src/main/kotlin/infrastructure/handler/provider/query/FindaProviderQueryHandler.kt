package com.nekgamebling.infrastructure.handler.provider.query

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.provider.query.FindaProviderQuery
import com.nekgamebling.application.port.inbound.provider.query.FindaProviderResponse
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.mapper.toAggregatorInfo
import infrastructure.persistence.exposed.mapper.toProvider
import infrastructure.persistence.exposed.table.AggregatorInfoTable
import infrastructure.persistence.exposed.table.GameTable
import infrastructure.persistence.exposed.table.ProviderTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class FindaProviderQueryHandler : QueryHandler<FindaProviderQuery, FindaProviderResponse> {

    override suspend fun handle(query: FindaProviderQuery): Result<FindaProviderResponse> = newSuspendedTransaction {
        // Count expressions for games
        val totalGamesCount = GameTable.id.count()
        val activeGamesCount = Case()
            .When(GameTable.active eq true, intLiteral(1))
            .Else(intLiteral(0))
            .sum()

        // Single query with join and aggregation
        val row = ProviderTable
            .innerJoin(AggregatorInfoTable, { ProviderTable.aggregatorId }, { AggregatorInfoTable.id })
            .leftJoin(GameTable, { ProviderTable.id }, { GameTable.providerId })
            .select(
                ProviderTable.columns +
                AggregatorInfoTable.columns +
                totalGamesCount +
                activeGamesCount
            )
            .where { ProviderTable.identity eq query.identity }
            .groupBy(*ProviderTable.columns.toTypedArray(), *AggregatorInfoTable.columns.toTypedArray())
            .firstOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Provider", query.identity))

        Result.success(
            FindaProviderResponse(
                provider = row.toProvider(),
                aggregator = row.toAggregatorInfo(),
                activeGames = row[activeGamesCount]?.toInt() ?: 0,
                totalGames = row[totalGamesCount].toInt()
            )
        )
    }
}
