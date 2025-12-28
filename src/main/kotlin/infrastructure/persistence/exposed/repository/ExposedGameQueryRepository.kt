package infrastructure.persistence.exposed.repository

import application.query.game.GameDetailsReadModel
import application.query.game.GameListReadModel
import application.query.game.GameQueryRepository
import application.query.game.GameSummaryReadModel
import domain.game.repository.GameFilter
import infrastructure.persistence.exposed.mapper.toCollection
import infrastructure.persistence.exposed.mapper.toGame
import infrastructure.persistence.exposed.mapper.toGameVariant
import infrastructure.persistence.exposed.mapper.toGameWithDetails
import infrastructure.persistence.exposed.mapper.toProvider
import infrastructure.persistence.exposed.table.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.json.contains
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Page
import shared.value.Pageable
import java.util.UUID

/**
 * Exposed implementation of GameQueryRepository.
 * Optimized for read operations with caching-friendly patterns.
 */
class ExposedGameQueryRepository : GameQueryRepository {

    override suspend fun findDetailsById(id: UUID): GameDetailsReadModel? = newSuspendedTransaction {
        buildFullGameQuery()
            .andWhere { GameTable.id eq id }
            .singleOrNull()
            ?.let { GameDetailsReadModel.from(it.toGameWithDetails()) }
    }

    override suspend fun findDetailsByIdentity(identity: String): GameDetailsReadModel? = newSuspendedTransaction {
        buildFullGameQuery()
            .andWhere { GameTable.identity eq identity }
            .singleOrNull()
            ?.let { GameDetailsReadModel.from(it.toGameWithDetails()) }
    }

    override suspend fun findDetailsBySymbol(symbol: String): GameDetailsReadModel? = newSuspendedTransaction {
        buildFullGameQuery()
            .andWhere { GameVariantTable.symbol eq symbol }
            .singleOrNull()
            ?.let { GameDetailsReadModel.from(it.toGameWithDetails()) }
    }

    override suspend fun findAll(pageable: Pageable, filter: GameFilter): Page<GameListReadModel> = newSuspendedTransaction {
        // Query 1: Count distinct game IDs
        val countQuery = buildBaseQueryForIds().applyFilters(filter)
        val totalCount = countQuery.withDistinct().count()

        if (totalCount == 0L) {
            return@newSuspendedTransaction Page.empty()
        }

        val totalPages = pageable.getTotalPages(totalCount)

        // Get paginated game IDs
        val gameIds = buildBaseQueryForIds()
            .applyFilters(filter)
            .withDistinct()
            .orderBy(GameTable.name to SortOrder.ASC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it[GameTable.id].value }

        if (gameIds.isEmpty()) {
            return@newSuspendedTransaction Page.empty()
        }

        // Query 2: Fetch games with details
        val gamesMap = linkedMapOf<UUID, GameDataBuilder>()
        buildBaseQuery()
            .andWhere { GameTable.id inList gameIds }
            .forEach { row ->
                val gameId = row[GameTable.id].value
                if (!gamesMap.containsKey(gameId)) {
                    gamesMap[gameId] = GameDataBuilder(
                        game = row.toGame(),
                        variant = row.toGameVariant(),
                        provider = row.toProvider()
                    )
                }
            }

        // Query 3: Batch-load collections
        val collectionsMap = CollectionGameTable
            .innerJoin(CollectionTable, { CollectionGameTable.categoryId }, { CollectionTable.id })
            .selectAll()
            .where { CollectionGameTable.gameId inList gameIds }
            .orderBy(CollectionGameTable.order to SortOrder.ASC)
            .groupBy { it[CollectionGameTable.gameId].value }
            .mapValues { (_, rows) -> rows.map { it.toCollection() } }

        // Build read models
        val items = gameIds.mapNotNull { gameId ->
            gamesMap[gameId]?.let { builder ->
                GameListReadModel.from(
                    game = builder.game,
                    variant = builder.variant,
                    provider = builder.provider,
                    collections = collectionsMap[gameId] ?: emptyList()
                )
            }
        }

        Page(
            items = items,
            totalPages = totalPages,
            totalItems = totalCount,
            currentPage = pageable.pageReal
        )
    }

    override suspend fun search(query: String, limit: Int): List<GameSummaryReadModel> = newSuspendedTransaction {
        GameTable
            .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
            .innerJoin(GameVariantTable, { GameVariantTable.gameId }, { GameTable.id })
            .select(GameTable.id, GameTable.identity, GameTable.name, GameVariantTable.symbol, ProviderTable.name)
            .where {
                (GameTable.name like "%$query%") or (GameTable.identity like "%$query%")
            }
            .andWhere { GameTable.active eq true }
            .withDistinct()
            .limit(limit)
            .map { row ->
                GameSummaryReadModel(
                    id = row[GameTable.id].value,
                    identity = row[GameTable.identity],
                    name = row[GameTable.name],
                    symbol = row[GameVariantTable.symbol],
                    providerName = row[ProviderTable.name]
                )
            }
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

    private fun buildBaseQuery(): Query {
        return GameTable
            .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
            .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
            .innerJoin(GameVariantTable, { GameVariantTable.gameId }, { GameTable.id }) {
                GameVariantTable.aggregator eq AggregatorInfoTable.aggregator
            }
            .selectAll()
    }

    private fun buildBaseQueryForIds(): Query {
        return GameTable
            .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
            .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
            .innerJoin(GameVariantTable, { GameVariantTable.gameId }, { GameTable.id }) {
                GameVariantTable.aggregator eq AggregatorInfoTable.aggregator
            }
            .select(GameTable.id, GameTable.name)
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
            val gameIdsInCollections = CollectionGameTable
                .innerJoin(CollectionTable, { CollectionGameTable.categoryId }, { CollectionTable.id })
                .select(CollectionGameTable.gameId)
                .where { CollectionTable.identity inList filter.collectionIdentities }
            andWhere { GameTable.id inSubQuery gameIdsInCollections }
        }

        if (filter.tags.isNotEmpty()) {
            andWhere { GameTable.tags.contains(filter.tags) }
        }

        filter.playerId?.let { playerId ->
            val favouriteGameIds = GameFavouriteTable
                .select(GameFavouriteTable.gameId)
                .where { GameFavouriteTable.playerId eq playerId }
            andWhere { GameTable.id inSubQuery favouriteGameIds }
        }
    }

    /**
     * Builder for collecting game data before creating read model.
     */
    private data class GameDataBuilder(
        val game: domain.game.model.Game,
        val variant: domain.game.model.GameVariant,
        val provider: domain.provider.model.Provider
    )
}
