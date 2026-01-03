package com.nekgamebling.infrastructure.handler.collection.query

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.collection.query.FindAllCollectionsQuery
import com.nekgamebling.application.port.inbound.collection.query.FindAllCollectionsResponse
import com.nekgamebling.application.port.inbound.collection.query.FindAllCollectionsResponse.CollectionItem
import infrastructure.persistence.exposed.mapper.toCollection
import infrastructure.persistence.exposed.table.CollectionGameTable
import infrastructure.persistence.exposed.table.CollectionTable
import infrastructure.persistence.exposed.table.GameTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Page

class FindAllCollectionsQueryHandler : QueryHandler<FindAllCollectionsQuery, FindAllCollectionsResponse> {

    override suspend fun handle(query: FindAllCollectionsQuery): Result<FindAllCollectionsResponse> = newSuspendedTransaction {
        // Build conditions
        val conditions = mutableListOf<Op<Boolean>>()

        if (query.query.isNotBlank()) {
            conditions += CollectionTable.identity.lowerCase() like "%${query.query.lowercase()}%"
        }

        if (query.active != null) {
            conditions += CollectionTable.active eq query.active
        }

        val whereCondition = if (conditions.isEmpty()) Op.TRUE else conditions.reduce { acc, op -> acc and op }

        // Count total items
        val totalItems = CollectionTable
            .selectAll()
            .where { whereCondition }
            .count()

        // Count expressions
        val gameCount = CollectionGameTable.gameId.count()
        val providerCount = GameTable.providerId.countDistinct()

        // Query with pagination, join, and aggregation
        val rows = CollectionTable
            .leftJoin(CollectionGameTable, { CollectionTable.id }, { CollectionGameTable.categoryId })
            .leftJoin(GameTable, { CollectionGameTable.gameId }, { GameTable.id })
            .select(
                CollectionTable.columns +
                gameCount +
                providerCount
            )
            .where { whereCondition }
            .groupBy(*CollectionTable.columns.toTypedArray())
            .orderBy(CollectionTable.order to SortOrder.ASC)
            .limit(query.pageable.sizeReal)
            .offset(query.pageable.offset)
            .toList()

        // Map to collection items
        val items = rows.map { row ->
            CollectionItem(
                collection = row.toCollection(),
                providerCount = row[providerCount].toInt(),
                gameCount = row[gameCount].toInt()
            )
        }

        Result.success(
            FindAllCollectionsResponse(
                result = Page(
                    items = items,
                    totalPages = query.pageable.getTotalPages(totalItems),
                    totalItems = totalItems,
                    currentPage = query.pageable.pageReal
                )
            )
        )
    }
}
