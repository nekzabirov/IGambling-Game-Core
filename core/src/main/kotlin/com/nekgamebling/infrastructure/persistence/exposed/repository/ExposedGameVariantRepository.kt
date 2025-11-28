package com.nekgamebling.infrastructure.persistence.exposed.repository

import com.nekgamebling.domain.game.model.GameVariant
import com.nekgamebling.domain.game.repository.GameVariantRepository
import com.nekgamebling.infrastructure.persistence.exposed.mapper.toGameVariant
import com.nekgamebling.infrastructure.persistence.exposed.table.GameVariantTable
import com.nekgamebling.shared.value.Aggregator
import com.nekgamebling.shared.value.Page
import com.nekgamebling.shared.value.Pageable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsertReturning
import java.util.UUID

/**
 * Exposed implementation of GameVariantRepository.
 */
class ExposedGameVariantRepository : GameVariantRepository {

    override suspend fun findById(id: UUID): GameVariant? = newSuspendedTransaction {
        GameVariantTable.selectAll()
            .where { GameVariantTable.id eq id }
            .singleOrNull()
            ?.toGameVariant()
    }

    override suspend fun findBySymbol(symbol: String, aggregator: Aggregator): GameVariant? = newSuspendedTransaction {
        GameVariantTable.selectAll()
            .where { GameVariantTable.symbol eq symbol and (GameVariantTable.aggregator eq aggregator) }
            .singleOrNull()
            ?.toGameVariant()
    }

    override suspend fun findByGameId(gameId: UUID): List<GameVariant> = newSuspendedTransaction {
        GameVariantTable.selectAll()
            .where { GameVariantTable.gameId eq gameId }
            .map { it.toGameVariant() }
    }

    override suspend fun findByAggregator(aggregator: Aggregator): List<GameVariant> = newSuspendedTransaction {
        GameVariantTable.selectAll()
            .where { GameVariantTable.aggregator eq aggregator }
            .map { it.toGameVariant() }
    }

    override suspend fun save(variant: GameVariant): GameVariant = newSuspendedTransaction {
        val row = GameVariantTable.upsertReturning(
            keys = arrayOf(GameVariantTable.symbol, GameVariantTable.aggregator),
            onUpdateExclude = listOf(GameVariantTable.createdAt, GameVariantTable.gameId),
        ) {
            it[gameId] = variant.gameId
            it[symbol] = variant.symbol
            it[name] = variant.name
            it[providerName] = variant.providerName
            it[aggregator] = variant.aggregator
            it[freeSpinEnable] = variant.freeSpinEnable
            it[freeChipEnable] = variant.freeChipEnable
            it[jackpotEnable] = variant.jackpotEnable
            it[demoEnable] = variant.demoEnable
            it[bonusBuyEnable] = variant.bonusBuyEnable
            it[locales] = variant.locales.map { l -> l.value }
            it[platforms] = variant.platforms.map { p -> p.name }
            it[playLines] = variant.playLines
        }.single()

        variant.copy(id = row[GameVariantTable.id].value, gameId = row[GameVariantTable.gameId]?.value)
    }

    override suspend fun saveAll(variants: List<GameVariant>): List<GameVariant> = newSuspendedTransaction {
        variants.map { save(it) }
    }

    override suspend fun update(variant: GameVariant): GameVariant = newSuspendedTransaction {
        GameVariantTable.update({ GameVariantTable.id eq variant.id }) {
            it[gameId] = variant.gameId
            it[symbol] = variant.symbol
            it[name] = variant.name
            it[providerName] = variant.providerName
            it[aggregator] = variant.aggregator
            it[freeSpinEnable] = variant.freeSpinEnable
            it[freeChipEnable] = variant.freeChipEnable
            it[jackpotEnable] = variant.jackpotEnable
            it[demoEnable] = variant.demoEnable
            it[bonusBuyEnable] = variant.bonusBuyEnable
            it[locales] = variant.locales.map { l -> l.value }
            it[platforms] = variant.platforms.map { p -> p.name }
            it[playLines] = variant.playLines
        }
        variant
    }

    override suspend fun delete(id: UUID): Boolean = newSuspendedTransaction {
        GameVariantTable.deleteWhere { GameVariantTable.id eq id } > 0
    }

    override suspend fun linkToGame(variantId: UUID, gameId: UUID): Boolean = newSuspendedTransaction {
        GameVariantTable.update({ GameVariantTable.id eq variantId }) {
            it[GameVariantTable.gameId] = gameId
        } > 0
    }

    override suspend fun findAll(pageable: Pageable): Page<GameVariant> = newSuspendedTransaction {
        val totalCount = GameVariantTable.selectAll().count()

        val items = GameVariantTable.selectAll()
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it.toGameVariant() }

        Page(
            items = items,
            totalPages = pageable.getTotalPages(totalCount),
            totalItems = totalCount,
            currentPage = pageable.pageReal
        )
    }
}
