package com.nekgamebling.infrastructure.persistence.exposed.repository

import com.nekgamebling.shared.value.Page
import com.nekgamebling.shared.value.Pageable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * Base repository providing common CRUD operations for Exposed tables.
 *
 * @param T The domain entity type
 * @param TTable The Exposed table type
 */
abstract class BaseExposedRepository<T, TTable : UUIDTable>(
    protected val table: TTable
) {
    /**
     * Maps a ResultRow to the domain entity.
     */
    protected abstract fun ResultRow.toEntity(): T

    /**
     * Find entity by ID.
     */
    open suspend fun findById(id: UUID): T? = newSuspendedTransaction {
        table.selectAll()
            .where { table.id eq id }
            .singleOrNull()
            ?.toEntity()
    }

    /**
     * Delete entity by ID.
     */
    open suspend fun delete(id: UUID): Boolean = newSuspendedTransaction {
        table.deleteWhere { table.id eq id } > 0
    }

    /**
     * Find all entities with pagination.
     */
    open suspend fun findAllPaged(pageable: Pageable): Page<T> = newSuspendedTransaction {
        val totalCount = table.selectAll().count()
        val items = table.selectAll()
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

    /**
     * Find single entity by column value.
     */
    protected suspend fun <V : Any> findOneBy(column: Column<V>, value: V): T? = newSuspendedTransaction {
        table.selectAll()
            .where { column eq value }
            .singleOrNull()
            ?.toEntity()
    }

    /**
     * Find single entity by reference column value.
     */
    protected suspend fun findOneByRef(column: Column<EntityID<UUID>>, value: UUID): T? = newSuspendedTransaction {
        table.selectAll()
            .where { column eq value }
            .singleOrNull()
            ?.toEntity()
    }

    /**
     * Find all entities by column value.
     */
    protected suspend fun <V : Any> findAllBy(column: Column<V>, value: V): List<T> = newSuspendedTransaction {
        table.selectAll()
            .where { column eq value }
            .map { it.toEntity() }
    }

    /**
     * Find all entities by reference column value.
     */
    protected suspend fun findAllByRef(column: Column<EntityID<UUID>>, value: UUID): List<T> = newSuspendedTransaction {
        table.selectAll()
            .where { column eq value }
            .map { it.toEntity() }
    }

    /**
     * Find all entities by nullable reference column value.
     */
    protected suspend fun findAllByNullableRef(column: Column<EntityID<UUID>?>, value: UUID): List<T> = newSuspendedTransaction {
        table.selectAll()
            .where { column eq value }
            .map { it.toEntity() }
    }

    /**
     * Check if entity exists by column value.
     */
    protected suspend fun <V : Any> existsBy(column: Column<V>, value: V): Boolean = newSuspendedTransaction {
        table.selectAll()
            .where { column eq value }
            .count() > 0
    }
}

/**
 * Interface for tables that have an identity column.
 */
interface IdentityTable {
    val identity: Column<String>
}

/**
 * Extension for repositories with identity-based tables.
 */
abstract class BaseExposedRepositoryWithIdentity<T, TTable>(
    table: TTable
) : BaseExposedRepository<T, TTable>(table) where TTable : UUIDTable, TTable : IdentityTable {

    open suspend fun findByIdentity(identity: String): T? = findOneBy(table.identity, identity)

    open suspend fun existsByIdentity(identity: String): Boolean = existsBy(table.identity, identity)
}
