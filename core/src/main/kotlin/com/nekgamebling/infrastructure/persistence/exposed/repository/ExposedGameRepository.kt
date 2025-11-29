package com.nekgamebling.infrastructure.persistence.exposed.repository

import com.nekgamebling.domain.game.model.Game
import com.nekgamebling.domain.game.model.GameWithDetails
import com.nekgamebling.domain.game.repository.GameFilter
import com.nekgamebling.domain.game.repository.GameListItem
import com.nekgamebling.domain.game.repository.GameRepository
import com.nekgamebling.infrastructure.persistence.exposed.mapper.*
import com.nekgamebling.infrastructure.persistence.exposed.table.*
import com.nekgamebling.shared.value.Page
import com.nekgamebling.shared.value.Pageable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.json.contains
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * Exposed implementation of GameRepository.
 */
class ExposedGameRepository : BaseExposedRepositoryWithIdentity<Game, GameTable>(GameTable), GameRepository {

    override fun ResultRow.toEntity(): Game = toGame()

    override suspend fun findBySymbol(symbol: String): Game? = newSuspendedTransaction {
        GameTable
            .innerJoin(GameVariantTable, { GameTable.id }, { GameVariantTable.gameId })
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
        val baseQuery = buildListQuery().applyFilters(filter)

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

                row.getOrNull(CollectionTable.id)?.let {
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
