package app.usecase

import domain.collection.model.Collection
import domain.collection.mapper.toCollection
import domain.collection.table.CollectionGameTable
import domain.collection.table.CollectionTable
import domain.game.table.GameTable
import core.db.paging
import core.ext.ilike
import core.ext.toJsonText
import core.model.Page
import core.model.Pageable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.case
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.collections.plus

class ListCollectionUsecase {
    suspend operator fun invoke(pageable: Pageable, filterBuilder: (Filter) -> Unit): Page<CategoryDto> =
        newSuspendedTransaction {
            val filter = Filter().apply(filterBuilder)

            fun Query.applyFilters() = apply {
                if (filter.query.isNotBlank()) {
                    andWhere {
                        CollectionTable.identity ilike "%${filter.query}%" or
                                (CollectionTable.name.toJsonText() ilike "%${filter.query}%")
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