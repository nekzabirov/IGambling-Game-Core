package com.nekgamebling.infrastructure.persistence.exposed.repository

import com.nekgamebling.domain.collection.model.Collection
import com.nekgamebling.domain.collection.model.CollectionGame
import com.nekgamebling.domain.collection.repository.CollectionRepository
import com.nekgamebling.infrastructure.persistence.exposed.mapper.toCollection
import com.nekgamebling.infrastructure.persistence.exposed.table.CollectionGameTable
import com.nekgamebling.infrastructure.persistence.exposed.table.CollectionTable
import com.nekgamebling.shared.value.Page
import com.nekgamebling.shared.value.Pageable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * Exposed implementation of CollectionRepository.
 */
class ExposedCollectionRepository : BaseExposedRepositoryWithIdentity<Collection, CollectionTable>(CollectionTable), CollectionRepository {

    override fun ResultRow.toEntity(): Collection = toCollection()

    override suspend fun save(collection: Collection): Collection = newSuspendedTransaction {
        val id = CollectionTable.insertAndGetId {
            it[identity] = collection.identity
            it[name] = collection.name
            it[images] = collection.images
            it[active] = collection.active
            it[order] = collection.order
        }
        collection.copy(id = id.value)
    }

    override suspend fun update(collection: Collection): Collection = newSuspendedTransaction {
        CollectionTable.update({ CollectionTable.id eq collection.id }) {
            it[identity] = collection.identity
            it[name] = collection.name
            it[images] = collection.images
            it[active] = collection.active
            it[order] = collection.order
        }
        collection
    }

    override suspend fun findAll(pageable: Pageable, activeOnly: Boolean): Page<Collection> = newSuspendedTransaction {
        val baseQuery = table.selectAll()
            .let { query ->
                if (activeOnly) query.andWhere { CollectionTable.active eq true }
                else query
            }

        val totalCount = baseQuery.count()
        val items = baseQuery
            .orderBy(CollectionTable.order to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it.toEntity() }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalCount),
            totalItems = totalCount,
            currentPage = pageable.pageReal
        )
    }

    override suspend fun addGame(collectionId: UUID, gameId: UUID, order: Int): Boolean = newSuspendedTransaction {
        val exists = CollectionGameTable.selectAll()
            .where { (CollectionGameTable.categoryId eq collectionId) and (CollectionGameTable.gameId eq gameId) }
            .count() > 0

        if (exists) return@newSuspendedTransaction true

        CollectionGameTable.insert {
            it[categoryId] = collectionId
            it[CollectionGameTable.gameId] = gameId
            it[CollectionGameTable.order] = order
        }
        true
    }

    override suspend fun removeGame(collectionId: UUID, gameId: UUID): Boolean = newSuspendedTransaction {
        CollectionGameTable.deleteWhere {
            (CollectionGameTable.categoryId eq collectionId) and (CollectionGameTable.gameId eq gameId)
        } > 0
    }

    override suspend fun updateGameOrder(collectionId: UUID, gameId: UUID, order: Int): Boolean = newSuspendedTransaction {
        CollectionGameTable.update({
            (CollectionGameTable.categoryId eq collectionId) and (CollectionGameTable.gameId eq gameId)
        }) {
            it[CollectionGameTable.order] = order
        } > 0
    }

    override suspend fun findGamesInCollection(collectionId: UUID): List<CollectionGame> = newSuspendedTransaction {
        CollectionGameTable.selectAll()
            .where { CollectionGameTable.categoryId eq collectionId }
            .orderBy(CollectionGameTable.order to SortOrder.ASC)
            .map { row ->
                CollectionGame(
                    collectionId = row[CollectionGameTable.categoryId].value,
                    gameId = row[CollectionGameTable.gameId].value,
                    order = row[CollectionGameTable.order]
                )
            }
    }

    override suspend fun findCollectionsForGame(gameId: UUID): List<Collection> = newSuspendedTransaction {
        CollectionTable
            .innerJoin(CollectionGameTable, { CollectionTable.id }, { CollectionGameTable.categoryId })
            .selectAll()
            .where { CollectionGameTable.gameId eq gameId }
            .map { it.toEntity() }
    }
}
