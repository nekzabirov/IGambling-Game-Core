package infrastructure.persistence.exposed.repository

import domain.game.model.Game
import domain.game.model.GameWithDetails
import domain.game.repository.GameFilter
import domain.game.repository.GameListItem
import domain.game.repository.GameRepository
import infrastructure.persistence.exposed.mapper.*
import infrastructure.persistence.exposed.table.*
import shared.value.Page
import shared.value.Pageable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.json.contains
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import domain.common.value.Aggregator
import java.util.UUID

/**
 * Exposed implementation of GameRepository.
 */
class ExposedGameRepository : BaseExposedRepositoryWithIdentity<Game, GameTable>(GameTable), GameRepository {

    override fun ResultRow.toEntity(): Game = toGame()

    override suspend fun findBySymbol(symbol: String, aggregator: Aggregator): Game? = newSuspendedTransaction {
        GameTable
            .innerJoin(
                GameVariantTable,
                { GameTable.id },
                { GameVariantTable.gameId },
                { GameVariantTable.aggregator eq aggregator })
            .selectAll()
            .where { GameVariantTable.symbol eq symbol }
            .singleOrNull()
            ?.toEntity()
    }

    override suspend fun findByNameAndProviderId(name: String, providerId: UUID): Game? = newSuspendedTransaction {
        table.selectAll()
            .where { GameTable.name eq name and (GameTable.providerId eq providerId) }
            .singleOrNull()
            ?.toEntity()
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
        // Optimized 3-query approach to avoid N+1 and cartesian explosion

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

        // Query 2: Fetch games with details (no collections)
        val gamesMap = linkedMapOf<UUID, GameListItem>()
        buildBaseQuery()
            .andWhere { GameTable.id inList gameIds }
            .forEach { row ->
                val gameId = row[GameTable.id].value
                if (!gamesMap.containsKey(gameId)) {
                    gamesMap[gameId] = GameListItem(
                        game = row.toGame(),
                        variant = row.toGameVariant(),
                        provider = row.toProvider(),
                        collections = linkedSetOf()  // Use Set for O(1) contains
                    )
                }
            }

        // Query 3: Batch-load collections for these games
        val collectionsMap = CollectionGameTable
            .innerJoin(CollectionTable, { CollectionGameTable.categoryId }, { CollectionTable.id })
            .selectAll()
            .where { CollectionGameTable.gameId inList gameIds }
            .orderBy(CollectionGameTable.order to SortOrder.ASC)
            .groupBy { it[CollectionGameTable.gameId].value }
            .mapValues { (_, rows) -> rows.map { it.toCollection() } }

        // Merge collections into games (O(1) lookup)
        collectionsMap.forEach { (gameId, collections) ->
            gamesMap[gameId]?.collections?.addAll(collections)
        }

        // Maintain order from gameIds
        val orderedItems = gameIds.mapNotNull { gamesMap[it] }

        Page(
            items = orderedItems,
            totalPages = totalPages,
            totalItems = totalCount,
            currentPage = pageable.pageReal
        )
    }

    /**
     * Base query without collection joins (for counting and game fetching).
     */
    private fun buildBaseQuery(): Query {
        return GameTable
            .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
            .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
            .innerJoin(GameVariantTable, { GameVariantTable.gameId }, { GameTable.id }) {
                GameVariantTable.aggregator eq AggregatorInfoTable.aggregator
            }
            .selectAll()
    }

    /**
     * Base query selecting only game IDs for pagination/counting.
     */
    private fun buildBaseQueryForIds(): Query {
        return GameTable
            .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
            .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
            .innerJoin(GameVariantTable, { GameVariantTable.gameId }, { GameTable.id }) {
                GameVariantTable.aggregator eq AggregatorInfoTable.aggregator
            }
            .select(GameTable.id, GameTable.name)
    }

    override suspend fun addTag(gameId: UUID, tag: String): Boolean = newSuspendedTransaction {
        val game = table.selectAll()
            .where { GameTable.id eq gameId }
            .singleOrNull()
            ?.toEntity() ?: return@newSuspendedTransaction false

        if (tag in game.tags) return@newSuspendedTransaction true

        GameTable.update({ GameTable.id eq gameId }) {
            it[tags] = game.tags + tag
        } > 0
    }

    override suspend fun removeTag(gameId: UUID, tag: String): Boolean = newSuspendedTransaction {
        val game = table.selectAll()
            .where { GameTable.id eq gameId }
            .singleOrNull()
            ?.toEntity() ?: return@newSuspendedTransaction false

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

    private fun Query.applyFilters(filter: GameFilter): Query = apply {
        if (filter.query.isNotBlank()) {
            andWhere {
                GameTable.name.ilike(filter.query) or GameTable.identity.ilike(filter.query)
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

        // Use subquery for collection filter (no longer joining CollectionTable)
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

        // Use subquery for favourite filter (no longer joining GameFavouriteTable)
        filter.playerId?.let { playerId ->
            val favouriteGameIds = GameFavouriteTable
                .select(GameFavouriteTable.gameId)
                .where { GameFavouriteTable.playerId eq playerId }
            andWhere { GameTable.id inSubQuery favouriteGameIds }
        }
    }
}
