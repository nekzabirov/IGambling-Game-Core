package infrastructure.handler.query

import application.port.inbound.QueryHandler
import application.port.inbound.query.*
import domain.collection.model.Collection
import infrastructure.persistence.exposed.mapper.toCollection
import infrastructure.persistence.exposed.table.CollectionGameTable
import infrastructure.persistence.exposed.table.CollectionTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Page
import java.util.UUID

/**
 * Query handler for listing collections with pagination.
 */
class ListCollectionsQueryHandler : QueryHandler<ListCollectionsQuery, Page<CollectionListReadModel>> {
    override suspend fun handle(query: ListCollectionsQuery): Page<CollectionListReadModel> = newSuspendedTransaction {
        val pageable = query.pageable

        val baseQuery = CollectionTable.selectAll()
            .let { q ->
                if (query.activeOnly) q.andWhere { CollectionTable.active eq true }
                else q
            }

        val totalCount = baseQuery.count()

        val collections = CollectionTable.selectAll()
            .let { q ->
                if (query.activeOnly) q.andWhere { CollectionTable.active eq true }
                else q
            }
            .orderBy(CollectionTable.order to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it.toCollection() }

        // Batch load game counts
        val collectionIds = collections.map { it.id }
        val countColumn = CollectionGameTable.gameId.count()
        val gameCounts = if (collectionIds.isNotEmpty()) {
            CollectionGameTable
                .select(CollectionGameTable.categoryId, countColumn)
                .where { CollectionGameTable.categoryId inList collectionIds }
                .groupBy(CollectionGameTable.categoryId)
                .associate { row ->
                    row[CollectionGameTable.categoryId].value to row[countColumn].toInt()
                }
        } else {
            emptyMap()
        }

        val items = collections.map { collection ->
            CollectionListReadModel.from(
                collection = collection,
                gameCount = gameCounts[collection.id] ?: 0
            )
        }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalCount),
            totalItems = totalCount,
            currentPage = pageable.pageReal
        )
    }
}

/**
 * Query handler for finding a collection by identity.
 */
class FindCollectionByIdentityQueryHandler : QueryHandler<FindCollectionByIdentityQuery, Collection?> {
    override suspend fun handle(query: FindCollectionByIdentityQuery): Collection? = newSuspendedTransaction {
        CollectionTable.selectAll()
            .where { CollectionTable.identity eq query.identity }
            .singleOrNull()
            ?.toCollection()
    }
}

/**
 * Query handler for finding a collection by ID.
 */
class FindCollectionByIdQueryHandler : QueryHandler<FindCollectionByIdQuery, Collection?> {
    override suspend fun handle(query: FindCollectionByIdQuery): Collection? = newSuspendedTransaction {
        CollectionTable.selectAll()
            .where { CollectionTable.id eq query.id }
            .singleOrNull()
            ?.toCollection()
    }
}
