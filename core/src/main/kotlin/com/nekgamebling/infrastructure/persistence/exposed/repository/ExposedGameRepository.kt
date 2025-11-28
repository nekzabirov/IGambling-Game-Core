package com.nekgamebling.infrastructure.persistence.exposed.repository

import com.nekgamebling.domain.game.model.Game
import com.nekgamebling.domain.game.model.GameVariant
import com.nekgamebling.domain.game.model.GameWithDetails
import com.nekgamebling.domain.game.repository.*
import com.nekgamebling.infrastructure.persistence.exposed.mapper.*
import com.nekgamebling.infrastructure.persistence.exposed.table.*
import com.nekgamebling.shared.value.Aggregator
import com.nekgamebling.shared.value.Page
import com.nekgamebling.shared.value.Pageable
import com.nekgamebling.shared.value.Platform
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.json.contains
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * Exposed implementation of GameRepository.
 */
class ExposedGameRepository : GameRepository {

    override suspend fun findById(id: UUID): Game? = newSuspendedTransaction {
        GameTable.selectAll()
            .where { GameTable.id eq id }
            .singleOrNull()
            ?.toGame()
    }

    override suspend fun findByIdentity(identity: String): Game? = newSuspendedTransaction {
        GameTable.selectAll()
            .where { GameTable.identity eq identity }
            .singleOrNull()
            ?.toGame()
    }

    override suspend fun findBySymbol(symbol: String): Game? = newSuspendedTransaction {
        GameTable
            .innerJoin(GameVariantTable, { GameTable.id }, { GameVariantTable.gameId })
            .selectAll()
            .where { GameVariantTable.symbol eq symbol }
            .singleOrNull()
            ?.toGame()
    }

    override suspend fun findByNameAndProviderId(name: String, providerId: UUID): Game? = newSuspendedTransaction {
        GameTable
            .selectAll()
            .where { GameTable.name eq name and (GameTable.providerId eq providerId) }
            .singleOrNull()
            ?.toGame()
    }

    override suspend fun save(game: Game): Game = newSuspendedTransaction {
        val id = GameTable.insertAndGetId {
            it[identity] = game.identity
            it[name] = game.name
            it[providerId] = game.providerId
            it[images] = game.images
            it[bonusBetEnable] = game.bonusBetEnable
            it[bonusWageringEnable] = game.bonusWageringEnable
            it[tags] = game.tags
            it[active] = game.active
        }
        game.copy(id = id.value)
    }

    override suspend fun update(game: Game): Game = newSuspendedTransaction {
        GameTable.update({ GameTable.id eq game.id }) {
            it[identity] = game.identity
            it[name] = game.name
            it[providerId] = game.providerId
            it[images] = game.images
            it[bonusBetEnable] = game.bonusBetEnable
            it[bonusWageringEnable] = game.bonusWageringEnable
            it[tags] = game.tags
            it[active] = game.active
        }
        game
    }

    override suspend fun delete(id: UUID): Boolean = newSuspendedTransaction {
        GameTable.deleteWhere { GameTable.id eq id } > 0
    }

    override suspend fun existsByIdentity(identity: String): Boolean = newSuspendedTransaction {
        GameTable.selectAll()
            .where { GameTable.identity eq identity }
            .count() > 0
    }

    override suspend fun findWithDetailsById(id: UUID): GameWithDetails? = newSuspendedTransaction {
        buildFullGameQuery()
            .andWhere { GameTable.id eq id }
            .singleOrNull()
            ?.toGameWithDetails()
    }

    override suspend fun findWithDetailsByIdentity(identity: String): GameWithDetails? = newSuspendedTransaction {
        buildFullGameQuery()
            .andWhere { GameTable.identity eq identity }
            .singleOrNull()
            ?.toGameWithDetails()
    }

    override suspend fun findWithDetailsBySymbol(symbol: String): GameWithDetails? = newSuspendedTransaction {
        buildFullGameQuery()
            .andWhere { GameVariantTable.symbol eq symbol }
            .singleOrNull()
            ?.toGameWithDetails()
    }

    override suspend fun findAll(pageable: Pageable, filter: GameFilter): Page<GameListItem> = newSuspendedTransaction {
        val baseQuery = buildListQuery()
            .applyFilters(filter)

        val totalCount = baseQuery.count()
        val totalPages = pageable.getTotalPages(totalCount)

        val gameMap = linkedMapOf<UUID, GameListItem>()

        baseQuery
            .orderBy(CollectionGameTable.order to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .forEach { row ->
                val gameId = row[GameTable.id].value
                val item = gameMap.getOrPut(gameId) {
                    GameListItem(
                        game = row.toGame(),
                        variant = row.toGameVariant(),
                        provider = row.toProvider(),
                        collections = mutableListOf()
                    )
                }

                // Add collection if present
                row.getOrNull(CollectionTable.id)?.let { collectionId ->
                    val collection = row.toCollection()
                    if (collection !in item.collections) {
                        (item.collections as MutableList).add(collection)
                    }
                }
            }

        Page(
            items = gameMap.values.toList(),
            totalPages = totalPages,
            totalItems = totalCount,
            currentPage = pageable.pageReal
        )
    }

    override suspend fun addTag(gameId: UUID, tag: String): Boolean = newSuspendedTransaction {
        val game = GameTable.selectAll()
            .where { GameTable.id eq gameId }
            .singleOrNull()
            ?.toGame() ?: return@newSuspendedTransaction false

        if (tag in game.tags) return@newSuspendedTransaction true

        GameTable.update({ GameTable.id eq gameId }) {
            it[tags] = game.tags + tag
        } > 0
    }

    override suspend fun removeTag(gameId: UUID, tag: String): Boolean = newSuspendedTransaction {
        val game = GameTable.selectAll()
            .where { GameTable.id eq gameId }
            .singleOrNull()
            ?.toGame() ?: return@newSuspendedTransaction false

        GameTable.update({ GameTable.id eq gameId }) {
            it[tags] = game.tags - tag
        } > 0
    }

    private fun buildFullGameQuery(): Query {
        return GameTable
            .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
            .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
            .innerJoin(GameVariantTable, { GameVariantTable.gameId }, { GameTable.id }) {
                GameVariantTable.aggregator eq AggregatorInfoTable.aggregator
            }
            .selectAll()
            .andWhere { GameTable.active eq true }
            .andWhere { ProviderTable.active eq true }
            .andWhere { AggregatorInfoTable.active eq true }
    }

    private fun buildListQuery(): Query {
        return GameTable
            .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
            .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
            .innerJoin(GameVariantTable, { GameVariantTable.gameId }, { GameTable.id }) {
                GameVariantTable.aggregator eq AggregatorInfoTable.aggregator
            }
            .leftJoin(CollectionGameTable, { CollectionGameTable.gameId }, { GameTable.id })
            .leftJoin(CollectionTable, { CollectionTable.id }, { CollectionGameTable.categoryId })
            .leftJoin(GameFavouriteTable, { GameFavouriteTable.gameId }, { GameTable.id })
            .selectAll()
    }

    private fun Query.applyFilters(filter: GameFilter): Query = apply {
        if (filter.query.isNotBlank()) {
            andWhere {
                (GameTable.name like "%${filter.query}%") or
                        (GameTable.identity like "%${filter.query}%")
            }
        }

        filter.active?.let { active ->
            andWhere { GameTable.active eq active }
            if (active) {
                andWhere { ProviderTable.active eq true }
            }
        }

        filter.bonusBet?.let { andWhere { GameTable.bonusBetEnable eq it } }
        filter.bonusWagering?.let { andWhere { GameTable.bonusWageringEnable eq it } }
        filter.freeSpinEnable?.let { andWhere { GameVariantTable.freeSpinEnable eq it } }
        filter.freeChipEnable?.let { andWhere { GameVariantTable.freeChipEnable eq it } }
        filter.jackpotEnable?.let { andWhere { GameVariantTable.jackpotEnable eq it } }
        filter.demoEnable?.let { andWhere { GameVariantTable.demoEnable eq it } }
        filter.bonusBuyEnable?.let { andWhere { GameVariantTable.bonusBuyEnable eq it } }

        if (filter.platforms.isNotEmpty()) {
            andWhere { GameVariantTable.platforms.contains(filter.platforms.map { it.name }) }
        }

        if (filter.providerIdentities.isNotEmpty()) {
            andWhere { ProviderTable.identity inList filter.providerIdentities }
        }

        if (filter.collectionIdentities.isNotEmpty()) {
            andWhere { CollectionTable.identity inList filter.collectionIdentities }
        }

        if (filter.tags.isNotEmpty()) {
            andWhere { GameTable.tags.contains(filter.tags) }
        }

        filter.playerId?.let { playerId ->
            andWhere { GameFavouriteTable.playerId eq playerId }
        }
    }
}

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
        variants.map { save(it)}
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

/**
 * Exposed implementation of GameFavouriteRepository.
 */
class ExposedGameFavouriteRepository : GameFavouriteRepository {

    override suspend fun add(playerId: String, gameId: UUID): Boolean = newSuspendedTransaction {
        if (exists(playerId, gameId)) return@newSuspendedTransaction true

        GameFavouriteTable.insert {
            it[GameFavouriteTable.playerId] = playerId
            it[GameFavouriteTable.gameId] = gameId
        }
        true
    }

    override suspend fun remove(playerId: String, gameId: UUID): Boolean = newSuspendedTransaction {
        GameFavouriteTable.deleteWhere {
            (GameFavouriteTable.playerId eq playerId) and (GameFavouriteTable.gameId eq gameId)
        } > 0
    }

    override suspend fun exists(playerId: String, gameId: UUID): Boolean = newSuspendedTransaction {
        GameFavouriteTable.selectAll()
            .where { (GameFavouriteTable.playerId eq playerId) and (GameFavouriteTable.gameId eq gameId) }
            .count() > 0
    }

    override suspend fun findByPlayer(playerId: String): List<UUID> = newSuspendedTransaction {
        GameFavouriteTable.selectAll()
            .where { GameFavouriteTable.playerId eq playerId }
            .map { it[GameFavouriteTable.gameId].value }
    }
}

/**
 * Exposed implementation of GameWonRepository.
 */
class ExposedGameWonRepository : GameWonRepository {

    override suspend fun save(gameId: UUID, playerId: String, amount: Int, currency: String): Boolean =
        newSuspendedTransaction {
            GameWonTable.insert {
                it[GameWonTable.gameId] = gameId
                it[GameWonTable.playerId] = playerId
                it[GameWonTable.amount] = amount
                it[GameWonTable.currency] = currency
            }
            true
        }
}
