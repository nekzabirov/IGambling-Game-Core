package app.usecase

import domain.mapper.toCollection
import domain.model.Collection
import domain.table.CollectionGameTable
import domain.table.CollectionTable
import domain.table.GameTable
import domain.table.base.paging
import domain.value.Page
import domain.value.Pageable
import domain.mapper.toCollection
import domain.table.base.paging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.case
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.json.contains
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.collections.plus

class ListCollectionUsecase {
    suspend operator fun invoke(pageable: domain.value.Pageable, filterBuilder: (Filter) -> Unit): domain.value.Page<CategoryDto> =
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

    data class CategoryDto(val category: domain.model.Collection, val activeGamesCount: Int, val totalGamesCount: Int)

    class Filter {
        var query: String = ""
            private set

        var active: Boolean? = null
            private set

        fun withQuery(query: String) = apply { this.query = query }

        fun withActive(active: Boolean) = apply { this.active = active }
    }
}