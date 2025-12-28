package application.query.game

import domain.aggregator.model.AggregatorInfo
import domain.collection.model.Collection
import domain.game.model.Game
import domain.game.model.GameVariant
import domain.game.model.GameWithDetails
import domain.provider.model.Provider
import kotlinx.serialization.Serializable
import shared.value.ImageMap
import domain.common.value.Locale
import domain.common.value.Platform
import java.util.UUID

/**
 * Read model for game details query.
 * This is a query-side projection optimized for reading.
 */
@Serializable
data class GameDetailsReadModel(
    @Serializable(with = shared.serializer.UUIDSerializer::class)
    val id: UUID,
    val identity: String,
    val name: String,
    val images: ImageMap,
    val bonusBetEnable: Boolean,
    val bonusWageringEnable: Boolean,
    val tags: List<String>,
    val symbol: String,
    val freeSpinEnable: Boolean,
    val freeChipEnable: Boolean,
    val jackpotEnable: Boolean,
    val demoEnable: Boolean,
    val bonusBuyEnable: Boolean,
    val locales: List<Locale>,
    val platforms: List<Platform>,
    val playLines: Int,
    val providerIdentity: String,
    val providerName: String,
    val aggregatorName: String
) {
    fun supportsLocale(locale: Locale): Boolean = locales.contains(locale)
    fun supportsPlatform(platform: Platform): Boolean = platforms.contains(platform)

    companion object {
        /**
         * Convert from domain GameWithDetails (backward compatibility).
         */
        fun from(details: GameWithDetails) = GameDetailsReadModel(
            id = details.id,
            identity = details.identity,
            name = details.name,
            images = details.images,
            bonusBetEnable = details.bonusBetEnable,
            bonusWageringEnable = details.bonusWageringEnable,
            tags = details.tags,
            symbol = details.symbol,
            freeSpinEnable = details.freeSpinEnable,
            freeChipEnable = details.freeChipEnable,
            jackpotEnable = details.jackpotEnable,
            demoEnable = details.demoEnable,
            bonusBuyEnable = details.bonusBuyEnable,
            locales = details.locales,
            platforms = details.platforms,
            playLines = details.playLines,
            providerIdentity = details.provider.identity,
            providerName = details.provider.name,
            aggregatorName = details.aggregator.aggregator.name
        )
    }
}

/**
 * Read model for game list items.
 * Flattened structure optimized for list views.
 */
@Serializable
data class GameListReadModel(
    @Serializable(with = shared.serializer.UUIDSerializer::class)
    val id: UUID,
    val identity: String,
    val name: String,
    val images: ImageMap,
    val tags: List<String>,
    val active: Boolean,
    val providerIdentity: String,
    val providerName: String,
    val symbol: String,
    val freeSpinEnable: Boolean,
    val freeChipEnable: Boolean,
    val jackpotEnable: Boolean,
    val demoEnable: Boolean,
    val bonusBuyEnable: Boolean,
    val platforms: List<Platform>,
    val collectionIdentities: List<String>
) {
    companion object {
        /**
         * Create from domain entities.
         */
        fun from(
            game: Game,
            variant: GameVariant,
            provider: Provider,
            collections: kotlin.collections.Collection<Collection>
        ) = GameListReadModel(
            id = game.id,
            identity = game.identity,
            name = game.name,
            images = game.images,
            tags = game.tags,
            active = game.active,
            providerIdentity = provider.identity,
            providerName = provider.name,
            symbol = variant.symbol,
            freeSpinEnable = variant.freeSpinEnable,
            freeChipEnable = variant.freeChipEnable,
            jackpotEnable = variant.jackpotEnable,
            demoEnable = variant.demoEnable,
            bonusBuyEnable = variant.bonusBuyEnable,
            platforms = variant.platforms,
            collectionIdentities = collections.map { it.identity }
        )
    }
}

/**
 * Summary read model for minimal game info.
 */
@Serializable
data class GameSummaryReadModel(
    @Serializable(with = shared.serializer.UUIDSerializer::class)
    val id: UUID,
    val identity: String,
    val name: String,
    val symbol: String,
    val providerName: String
)
