package com.nekgamebling.infrastructure.handler.aggregator.query

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.aggregator.FindAllAggregatorQuery
import com.nekgamebling.application.port.inbound.aggregator.FindAllAggregatorResponse
import infrastructure.persistence.exposed.mapper.toAggregatorInfo
import infrastructure.persistence.exposed.table.AggregatorInfoTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Page

class FindAllAggregatorQueryHandler : QueryHandler<FindAllAggregatorQuery, FindAllAggregatorResponse> {

    override suspend fun handle(query: FindAllAggregatorQuery): Result<FindAllAggregatorResponse> = newSuspendedTransaction {
        // Build conditions
        val conditions = mutableListOf<Op<Boolean>>()

        if (query.query.isNotBlank()) {
            conditions += AggregatorInfoTable.identity.lowerCase() like "%${query.query.lowercase()}%"
        }

        if (query.active != null) {
            conditions += AggregatorInfoTable.active eq query.active
        }

        val whereCondition = if (conditions.isEmpty()) Op.TRUE else conditions.reduce { acc, op -> acc and op }

        // Count total items
        val totalItems = AggregatorInfoTable
            .selectAll()
            .where { whereCondition }
            .count()

        // Query with pagination
        val items = AggregatorInfoTable
            .selectAll()
            .where { whereCondition }
            .orderBy(AggregatorInfoTable.identity to SortOrder.ASC)
            .limit(query.pageable.sizeReal)
            .offset(query.pageable.offset)
            .map { it.toAggregatorInfo() }

        Result.success(
            FindAllAggregatorResponse(
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
