package infrastructure.persistence.exposed.repository

import domain.game.model.GameVariant
import domain.game.model.GameVariantWithDetail
import domain.game.repository.GameVariantFilter
import domain.game.repository.GameVariantRepository
import infrastructure.persistence.exposed.mapper.toGame
import infrastructure.persistence.exposed.mapper.toGameVariant
import infrastructure.persistence.exposed.mapper.toProvider
import infrastructure.persistence.exposed.table.GameTable
import infrastructure.persistence.exposed.table.GameVariantTable
import infrastructure.persistence.exposed.table.ProviderTable
import domain.common.value.Aggregator
import shared.value.Page
import shared.value.Pageable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import infrastructure.persistence.exposed.repository.ilike
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.or
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

    override suspend fun findAllWithDetails(
        filter: GameVariantFilter,
        pageable: Pageable
    ): Page<GameVariantWithDetail> = newSuspendedTransaction {
        val query = GameVariantTable
            .leftJoin(GameTable, onColumn = { GameVariantTable.gameId }, otherColumn = { GameTable.id })
            .leftJoin(ProviderTable, onColumn = { GameTable.providerId }, otherColumn = { ProviderTable.id })
            .selectAll()
            .apply {
                if (filter.query.isNotBlank()) {
                    andWhere {
                        GameVariantTable.name.ilike(filter.query) or
                                GameVariantTable.providerName.ilike(filter.query) or
                                GameVariantTable.symbol.ilike(filter.query)
                    }
                }

                filter.aggregator?.also { aggregator ->
                    andWhere { GameVariantTable.aggregator eq aggregator }
                }

                filter.gameIdentity?.also { gameIdentity ->
                    andWhere { GameTable.identity eq gameIdentity }
                }
            }

        val totalCount = query.count()

        val items = query
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map {
                GameVariantWithDetail(
                    variant = it.toGameVariant(),
                    game = if (it.getOrNull(GameTable.id) != null) it.toGame() else null,
                    provider = if (it.getOrNull(ProviderTable.id) != null) it.toProvider() else null
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
