package com.nekgamebling.domain.game.repository

import com.nekgamebling.domain.collection.model.Collection
import com.nekgamebling.domain.game.model.Game
import com.nekgamebling.domain.game.model.GameVariant
import com.nekgamebling.domain.game.model.GameWithDetails
import com.nekgamebling.shared.value.Aggregator
import com.nekgamebling.shared.value.Page
import com.nekgamebling.shared.value.Pageable
import com.nekgamebling.shared.value.Platform
import java.util.UUID

/**
 * Repository interface for Game entity operations.
 */
interface GameRepository {
    suspend fun findById(id: UUID): Game?

    suspend fun findByIdentity(identity: String): Game?

    suspend fun findBySymbol(symbol: String): Game?

    suspend fun findByNameAndProviderId(name: String, providerId: UUID): Game?

    suspend fun save(game: Game): Game

    suspend fun update(game: Game): Game

    suspend fun delete(id: UUID): Boolean

    suspend fun existsByIdentity(identity: String): Boolean

    /**
     * Find game with all related details (provider, aggregator, variant).
     */
    suspend fun findWithDetailsById(id: UUID): GameWithDetails?

    suspend fun findWithDetailsByIdentity(identity: String): GameWithDetails?

    suspend fun findWithDetailsBySymbol(symbol: String): GameWithDetails?

    /**
     * List games with filtering and pagination.
     */
    suspend fun findAll(pageable: Pageable, filter: GameFilter): Page<GameListItem>

    /**
     * Tag management.
     */
    suspend fun addTag(gameId: UUID, tag: String): Boolean
    suspend fun removeTag(gameId: UUID, tag: String): Boolean
}

/**
 * Filter criteria for game queries.
 */
data class GameFilter(
    val query: String = "",
    val active: Boolean? = null,
    val bonusBet: Boolean? = null,
    val bonusWagering: Boolean? = null,
    val freeSpinEnable: Boolean? = null,
    val freeChipEnable: Boolean? = null,
    val jackpotEnable: Boolean? = null,
    val demoEnable: Boolean? = null,
    val bonusBuyEnable: Boolean? = null,
    val platforms: List<Platform> = emptyList(),
    val providerIdentities: List<String> = emptyList(),
    val collectionIdentities: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val playerId: String? = null
) {
    class Builder {
        private var query: String = ""
        private var active: Boolean? = null
        private var bonusBet: Boolean? = null
        private var bonusWagering: Boolean? = null
        private var freeSpinEnable: Boolean? = null
        private var freeChipEnable: Boolean? = null
        private var jackpotEnable: Boolean? = null
        private var demoEnable: Boolean? = null
        private var bonusBuyEnable: Boolean? = null
        private val platforms = mutableListOf<Platform>()
        private val providerIdentities = mutableListOf<String>()
        private val collectionIdentities = mutableListOf<String>()
        private val tags = mutableListOf<String>()
        private var playerId: String? = null

        fun query(query: String) = apply { this.query = query }
        fun active(active: Boolean?) = apply { this.active = active }
        fun bonusBet(bonusBet: Boolean?) = apply { this.bonusBet = bonusBet }
        fun bonusWagering(bonusWagering: Boolean?) = apply { this.bonusWagering = bonusWagering }
        fun freeSpinEnable(freeSpinEnable: Boolean?) = apply { this.freeSpinEnable = freeSpinEnable }
        fun freeChipEnable(freeChipEnable: Boolean?) = apply { this.freeChipEnable = freeChipEnable }
        fun jackpotEnable(jackpotEnable: Boolean?) = apply { this.jackpotEnable = jackpotEnable }
        fun demoEnable(demoEnable: Boolean?) = apply { this.demoEnable = demoEnable }
        fun bonusBuyEnable(bonusBuyEnable: Boolean?) = apply { this.bonusBuyEnable = bonusBuyEnable }
        fun platform(platform: Platform) = apply { platforms.add(platform) }
        fun platforms(platforms: List<Platform>) = apply { this.platforms.addAll(platforms) }
        fun providerIdentity(identity: String) = apply { providerIdentities.add(identity) }
        fun collectionIdentity(identity: String) = apply { collectionIdentities.add(identity) }
        fun tag(tag: String) = apply { tags.add(tag) }
        fun playerId(playerId: String?) = apply { this.playerId = playerId }

        fun build() = GameFilter(
            query = query,
            active = active,
            bonusBet = bonusBet,
            bonusWagering = bonusWagering,
            freeSpinEnable = freeSpinEnable,
            freeChipEnable = freeChipEnable,
            jackpotEnable = jackpotEnable,
            demoEnable = demoEnable,
            bonusBuyEnable = bonusBuyEnable,
            platforms = platforms.toList(),
            providerIdentities = providerIdentities.toList(),
            collectionIdentities = collectionIdentities.toList(),
            tags = tags.toList(),
            playerId = playerId
        )
    }

    companion object {
        fun builder() = Builder()
        val EMPTY = GameFilter()
    }
}

/**
 * Game list item containing game with variant, provider and collections.
 */
data class GameListItem(
    val game: Game,
    val variant: GameVariant,
    val provider: com.nekgamebling.domain.provider.model.Provider,
    val collections: List<Collection> = emptyList()
)

/**
 * Repository interface for GameVariant entity operations.
 */
interface GameVariantRepository {
    suspend fun findById(id: UUID): GameVariant?

    suspend fun findBySymbol(symbol: String, aggregator: Aggregator): GameVariant?

    suspend fun findByGameId(gameId: UUID): List<GameVariant>

    suspend fun findByAggregator(aggregator: Aggregator): List<GameVariant>

    suspend fun save(variant: GameVariant): GameVariant

    suspend fun saveAll(variants: List<GameVariant>): List<GameVariant>

    suspend fun update(variant: GameVariant): GameVariant

    suspend fun delete(id: UUID): Boolean

    suspend fun linkToGame(variantId: UUID, gameId: UUID): Boolean

    suspend fun findAll(pageable: Pageable): Page<GameVariant>
}

/**
 * Repository interface for game favorites.
 */
interface GameFavouriteRepository {
    suspend fun add(playerId: String, gameId: UUID): Boolean
    suspend fun remove(playerId: String, gameId: UUID): Boolean
    suspend fun exists(playerId: String, gameId: UUID): Boolean
    suspend fun findByPlayer(playerId: String): List<UUID>
}

/**
 * Repository interface for game wins.
 */
interface GameWonRepository {
    suspend fun save(gameId: UUID, playerId: String, amount: Int, currency: String): Boolean
}
