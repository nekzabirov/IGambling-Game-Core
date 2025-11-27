package app.usecase

import core.model.Page
import core.model.Pageable
import core.model.Platform
import domain.aggregator.table.AggregatorInfoTable
import domain.collection.mapper.toCollection
import domain.collection.model.Collection
import domain.collection.table.CollectionGameTable
import domain.collection.table.CollectionTable
import domain.game.mapper.toGame
import domain.game.mapper.toGameVariant
import domain.game.model.Game
import domain.game.model.GameVariant
import domain.game.table.GameFavouriteTable
import domain.game.table.GameTable
import domain.game.table.GameVariantTable
import domain.provider.mapper.toProvider
import domain.provider.model.Provider
import domain.provider.table.ProviderTable
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.json.contains
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import kotlin.collections.isNotEmpty

class ListGameUsecase {
    suspend operator fun invoke(pageable: Pageable, filterBuilder: Filter.() -> Unit): Page<GameDto> {
        return newSuspendedTransaction {
            val filter = Filter().apply(filterBuilder)

            fun Query.applyFilters() = apply {
                if (filter.query.isNotBlank()) {
                    andWhere { (GameTable.name like "%${filter.query}%") or (GameTable.identity like "%${filter.query}%") }
                }

                filter.active?.let {
                    andWhere { GameTable.active eq it }

                    if (it)
                        andWhere { ProviderTable.active eq true }
                }

                filter.bonusBet?.let {
                    andWhere { GameTable.bonusBetEnable eq it }
                }

                filter.bonusWagering?.let {
                    andWhere { GameTable.bonusWageringEnable eq it }
                }

                filter.freeSpinEnable?.let {
                    andWhere { GameVariantTable.freeSpinEnable eq it }
                }

                filter.freeChipEnable?.let {
                    andWhere { GameVariantTable.freeChipEnable eq it }
                }

                filter.jackpotEnable?.let {
                    andWhere { GameVariantTable.jackpotEnable eq it }
                }

                filter.demoEnable?.let {
                    andWhere { GameVariantTable.demoEnable eq it }
                }

                filter.bonusBuyEnable?.let {
                    andWhere { GameVariantTable.bonusBuyEnable eq it }
                }

                if (filter.platforms.isNotEmpty()) {
                    andWhere { GameVariantTable.platforms.contains(filter.platforms) }
                }

                if (filter.providerIdentity.isNotEmpty()) {
                    andWhere { ProviderTable.identity inList filter.providerIdentity }
                }

                if (filter.categoryIdentity.isNotEmpty()) {
                    andWhere { CollectionTable.identity inList filter.categoryIdentity }
                }

                if (filter.tags.isNotEmpty()) {
                    andWhere { GameTable.tags.contains(filter.tags) }
                }

                filter.playerId?.let { playerId ->
                    andWhere { GameFavouriteTable.playerId eq playerId }
                }
            }

            val joins = GameTable
                .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
                .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
                .innerJoin(
                    GameVariantTable,
                    { GameVariantTable.gameId },
                    { GameTable.id },
                    { GameVariantTable.aggregator eq AggregatorInfoTable.aggregator })
                .leftJoin(CollectionGameTable, { CollectionGameTable.gameId }, { GameTable.id })
                .leftJoin(CollectionTable, { CollectionTable.id }, { CollectionGameTable.categoryId })
                .leftJoin(GameFavouriteTable, { GameFavouriteTable.gameId }, { GameTable.id })

            val rows = joins.selectAll()
                .applyFilters()
                .orderBy(CollectionGameTable.order to SortOrder.ASC)
                .groupBy(
                    GameTable.id,
                    GameTable.createdAt,
                    GameTable.updatedAt,
                    GameTable.identity,
                    GameTable.name,
                    GameTable.providerId,
                    GameTable.images,
                    GameTable.bonusBetEnable,
                    GameTable.bonusWageringEnable,
                    GameTable.tags,
                    GameTable.active,
                    ProviderTable.id,
                    ProviderTable.createdAt,
                    ProviderTable.updatedAt,
                    ProviderTable.identity,
                    ProviderTable.name,
                    ProviderTable.order,
                    ProviderTable.aggregatorId,
                    ProviderTable.active,
                    GameVariantTable.id,
                    GameVariantTable.createdAt,
                    GameVariantTable.updatedAt,
                    GameVariantTable.gameId,
                    GameVariantTable.symbol,
                    GameVariantTable.name,
                    GameVariantTable.providerName,
                    GameVariantTable.aggregator,
                    GameVariantTable.playLines,
                    GameVariantTable.freeSpinEnable,
                    GameVariantTable.freeChipEnable,
                    GameVariantTable.jackpotEnable,
                    GameVariantTable.demoEnable,
                    GameVariantTable.bonusBuyEnable,
                    GameVariantTable.locales,
                    GameVariantTable.platforms,
                    CollectionGameTable.categoryId,
                    CollectionGameTable.gameId,
                    CollectionGameTable.order,
                    CollectionTable.id,
                    CollectionTable.createdAt,
                    CollectionTable.updatedAt,
                    CollectionTable.identity,
                    CollectionTable.name,
                    CollectionTable.active,
                    CollectionTable.order,
                    AggregatorInfoTable.id,
                    AggregatorInfoTable.identity,
                    AggregatorInfoTable.config,
                    AggregatorInfoTable.aggregator,
                    GameFavouriteTable.playerId,
                    GameFavouriteTable.gameId,
                )

            val gameSet = linkedMapOf<UUID, GameDto>()

            for (row in rows) {
                val game = gameSet.getOrPut(row[GameTable.id].value) {
                    val game = row.toGame()
                    val variant = row.toGameVariant()
                    val provider = row.toProvider()

                    GameDto(game, variant, provider)
                }

                if (row[CollectionGameTable.categoryId] != null) {
                    game.categories.add(row.toCollection())
                }
            }

            //TODO: Refactor this to use the same query
            val items = gameSet
                .values
                .toList()
                .let {
                    if (it.size < pageable.size) it
                    else it.subList(pageable.offset.toInt(), pageable.sizeReal.toInt())
                }

            val totalPages = pageable.getTotalPage(gameSet.size.toLong())

            Page(items = items, totalPages = totalPages)
        }
    }

    class Filter {
        var query: String = ""
            private set

        var active: Boolean? = null
            private set

        var bonusBet: Boolean? = null
            private set

        var bonusWagering: Boolean? = null
            private set

        var freeSpinEnable: Boolean? = null
            private set

        var freeChipEnable: Boolean? = null
            private set

        var jackpotEnable: Boolean? = null
            private set

        var demoEnable: Boolean? = null
            private set

        var bonusBuyEnable: Boolean? = null
            private set

        private val _platforms = mutableListOf<Platform>()
        val platforms: List<Platform>
            get() = _platforms

        private val _providerIdentity: MutableList<String> = mutableListOf()
        val providerIdentity: List<String>
            get() = _providerIdentity

        private val _categoryIdentity: MutableList<String> = mutableListOf()
        val categoryIdentity: List<String>
            get() = _categoryIdentity

        private val _tags = mutableListOf<String>()
        val tags: List<String>
            get() = _tags

        var playerId: String? = null

        fun withQuery(query: String) = apply { this.query = query }

        fun withActive(active: Boolean?) = apply { this.active = active }

        fun withBonusBet(bonusBet: Boolean?) = apply { this.bonusBet = bonusBet }

        fun withBonusWagering(bonusWagering: Boolean?) = apply { this.bonusWagering = bonusWagering }

        fun withFreeSpinEnable(freeSpinEnable: Boolean?) = apply { this.freeSpinEnable = freeSpinEnable }

        fun withFreeChipEnable(freeChipEnable: Boolean?) = apply { this.freeChipEnable = freeChipEnable }

        fun withJackpotEnable(jackpotEnable: Boolean?) = apply { this.jackpotEnable = jackpotEnable }

        fun withDemoEnable(demoEnable: Boolean?) = apply { this.demoEnable = demoEnable }

        fun withBonusBuyEnable(bonusBuyEnable: Boolean?) = apply { this.bonusBuyEnable = bonusBuyEnable }

        fun withPlatform(platform: Platform) = apply { _platforms.add(platform) }

        fun withProviderIdentity(providerIdentity: String) = apply { _providerIdentity.add(providerIdentity) }

        fun withCategoryIdentity(categoryIdentity: String) = apply { _categoryIdentity.add(categoryIdentity) }

        fun withTag(tag: String) = apply { _tags.add(tag) }

        fun withPlayer(playerId: String) = apply { this.playerId = playerId }
    }

    data class GameDto(
        val game: Game,
        val variant: GameVariant,
        val provider: Provider,
        val categories: MutableList<Collection> = mutableListOf()
    )
}