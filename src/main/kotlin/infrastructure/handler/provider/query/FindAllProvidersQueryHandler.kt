package com.nekgamebling.infrastructure.handler.provider.query

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.provider.query.FindAllProvidersQuery
import com.nekgamebling.application.port.inbound.provider.query.FindAllProvidersResponse
import com.nekgamebling.application.port.inbound.provider.query.FindAllProvidersResponse.ProviderItem
import infrastructure.persistence.exposed.mapper.toAggregatorInfo
import infrastructure.persistence.exposed.mapper.toProvider
import infrastructure.persistence.exposed.table.AggregatorInfoTable
import infrastructure.persistence.exposed.table.GameTable
import infrastructure.persistence.exposed.table.ProviderTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Page

class FindAllProvidersQueryHandler : QueryHandler<FindAllProvidersQuery, FindAllProvidersResponse> {

    override suspend fun handle(query: FindAllProvidersQuery): Result<FindAllProvidersResponse> = newSuspendedTransaction {
        // Count expressions for games
        val totalGamesCount = GameTable.id.count()
        val activeGamesCount = Case()
            .When(GameTable.active eq true, intLiteral(1))
            .Else(intLiteral(0))
            .sum()

        // Build conditions
        val conditions = mutableListOf<Op<Boolean>>()

        if (query.query.isNotBlank()) {
            conditions += (ProviderTable.name.lowerCase() like "%${query.query.lowercase()}%") or
                    (ProviderTable.identity.lowerCase() like "%${query.query.lowercase()}%")
        }

        if (query.active != null) {
            conditions += ProviderTable.active eq query.active
        }

        if (query.aggregatorIdentity != null) {
            conditions += AggregatorInfoTable.identity eq query.aggregatorIdentity
        }

        val whereCondition = if (conditions.isEmpty()) Op.TRUE else conditions.reduce { acc, op -> acc and op }

        // Count total items
        val totalItems = ProviderTable
            .innerJoin(AggregatorInfoTable, { ProviderTable.aggregatorId }, { AggregatorInfoTable.id })
            .selectAll()
            .where { whereCondition }
            .count()

        // Query with pagination, join, and aggregation
        val rows = ProviderTable
            .innerJoin(AggregatorInfoTable, { ProviderTable.aggregatorId }, { AggregatorInfoTable.id })
            .leftJoin(GameTable, { ProviderTable.id }, { GameTable.providerId })
            .select(
                ProviderTable.columns +
                AggregatorInfoTable.columns +
                totalGamesCount +
                activeGamesCount
            )
            .where { whereCondition }
            .groupBy(*ProviderTable.columns.toTypedArray(), *AggregatorInfoTable.columns.toTypedArray())
            .orderBy(ProviderTable.order to SortOrder.ASC)
            .limit(query.pageable.sizeReal)
            .offset(query.pageable.offset)
            .toList()

        // Map to provider items
        val items = rows.map { row ->
            ProviderItem(
                provider = row.toProvider(),
                aggregatorIdentity = row[AggregatorInfoTable.identity],
                activeGames = row[activeGamesCount]?.toInt() ?: 0,
                totalGames = row[totalGamesCount].toInt()
            )
        }

        // Collect unique aggregators from results
        val aggregators = rows
            .map { it.toAggregatorInfo() }
            .distinctBy { it.id }

        Result.success(
            FindAllProvidersResponse(
                result = Page(
                    items = items,
                    totalPages = query.pageable.getTotalPages(totalItems),
                    totalItems = totalItems,
                    currentPage = query.pageable.pageReal
                ),
                aggregators = aggregators
            )
        )
    }
}
