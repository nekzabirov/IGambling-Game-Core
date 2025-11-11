package com.nekzabirov.catalog.usecase

import com.nekzabirov.catalog.mapper.toCollection
import com.nekzabirov.catalog.model.Collection
import com.nekzabirov.catalog.table.CollectionGameTable
import com.nekzabirov.catalog.table.CollectionTable
import com.nekzabirov.catalog.table.GameTable
import com.nekzabirov.catalog.table.base.paging
import com.nekzabirov.catalog.value.Page
import com.nekzabirov.catalog.value.Pageable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.case
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.json.contains
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ListCollectionUsecase {
    suspend operator fun invoke(pageable: Pageable, filterBuilder: (Filter) -> Unit): Page<CategoryDto> =
        newSuspendedTransaction {
            val filter = Filter().apply(filterBuilder)

            fun Query.applyFilters() = apply {
                if (filter.query.isNotBlank()) {
                    andWhere {
                        CollectionTable.identity like "%${filter.query}%" or
                                CollectionTable.name.contains(filter.query)
                    }
                }

                filter.active?.also { isActive ->
                    andWhere { CollectionTable.active eq isActive }
                }
            }

            val activeSumExpr = case()
                .When(GameTable.active eq true, intLiteral(1))
                .Else(intLiteral(0))
                .sum()
                .alias("active_sum")

            val totalCountExpr = GameTable.id.count().alias("total_count")

            CollectionTable
                .leftJoin(
                    CollectionGameTable,
                    onColumn = { CollectionGameTable.categoryId },
                    otherColumn = { CollectionTable.id })
                .leftJoin(GameTable, onColumn = { CollectionGameTable.gameId }, otherColumn = { GameTable.id })
                .select(CollectionTable.columns + activeSumExpr + totalCountExpr)
                .applyFilters()
                .groupBy(CollectionTable.id, CollectionTable.name, CollectionTable.identity)
                .orderBy(CollectionTable.order to SortOrder.ASC)
                .paging(pageable)
                .map { resultRow ->
                    val activeGamesCount = resultRow[activeSumExpr] ?: 0
                    val totalGamesCount = resultRow[totalCountExpr]

                    val category = resultRow.toCollection()

                    CategoryDto(category, activeGamesCount, totalGamesCount.toInt())
                }
        }

    data class CategoryDto(val category: Collection, val activeGamesCount: Int, val totalGamesCount: Int)

    class Filter {
        var query: String = ""
            private set

        var active: Boolean? = null
            private set

        fun withQuery(query: String) = apply { this.query = query }

        fun withActive(active: Boolean) = apply { this.active = active }
    }
}