package com.nekgamebling.application.usecase.aggregator

import com.nekgamebling.application.port.outbound.AggregatorAdapterRegistry
import com.nekgamebling.domain.aggregator.repository.AggregatorRepository
import com.nekgamebling.domain.common.error.AggregatorNotSupportedError
import com.nekgamebling.domain.common.error.NotFoundError
import com.nekgamebling.domain.game.model.Game
import com.nekgamebling.domain.game.model.GameVariant
import com.nekgamebling.domain.game.repository.GameRepository
import com.nekgamebling.domain.game.repository.GameVariantRepository
import com.nekgamebling.domain.provider.model.Provider
import com.nekgamebling.domain.provider.repository.ProviderRepository
import com.nekgamebling.shared.value.ImageMap
import com.nekgamebling.shared.value.Platform
import java.util.UUID

/**
 * Result of game sync operation.
 */
data class SyncGameResult(
    val gameCount: Int,
    val providerCount: Int
)

/**
 * Use case for syncing games from an aggregator.
 */
class SyncGameUsecase(
    private val aggregatorRepository: AggregatorRepository,
    private val providerRepository: ProviderRepository,
    private val gameRepository: GameRepository,
    private val gameVariantRepository: GameVariantRepository,
    private val aggregatorRegistry: AggregatorAdapterRegistry
) {
    suspend operator fun invoke(aggregatorIdentity: String): Result<SyncGameResult> {
        val aggregatorInfo = aggregatorRepository.findByIdentity(aggregatorIdentity)
            ?: return Result.failure(NotFoundError("Aggregator", aggregatorIdentity))

        val factory = aggregatorRegistry.getFactory(aggregatorInfo.aggregator)
            ?: return Result.failure(AggregatorNotSupportedError(aggregatorInfo.aggregator.name))

        val gameSyncAdapter = factory.createGameSyncAdapter(aggregatorInfo)

        val games = gameSyncAdapter.listGames(aggregatorInfo).getOrElse {
            return Result.failure(it)
        }

        var gameCount = 0
        val providerNames = mutableSetOf<String>()

        for (aggregatorGame in games) {
            providerNames.add(aggregatorGame.providerName)

            // Find or create provider
            val providerIdentity = aggregatorGame.providerName.lowercase().replace(" ", "-")
            var provider = providerRepository.findByIdentity(providerIdentity)

            if (provider == null) {
                provider = providerRepository.save(
                    Provider(
                        id = UUID.randomUUID(),
                        identity = providerIdentity,
                        name = aggregatorGame.providerName,
                        images = ImageMap.EMPTY,
                        aggregatorId = aggregatorInfo.id
                    )
                )
            }

            // Find or create game variant
            var variant = gameVariantRepository.findBySymbol(aggregatorGame.symbol)

            if (variant == null) {
                variant = gameVariantRepository.save(
                    GameVariant(
                        id = UUID.randomUUID(),
                        symbol = aggregatorGame.symbol,
                        name = aggregatorGame.name,
                        providerName = aggregatorGame.providerName,
                        aggregator = aggregatorInfo.aggregator,
                        freeSpinEnable = aggregatorGame.freeSpinEnable,
                        freeChipEnable = aggregatorGame.freeChipEnable,
                        jackpotEnable = aggregatorGame.jackpotEnable,
                        demoEnable = aggregatorGame.demoEnable,
                        bonusBuyEnable = aggregatorGame.bonusBuyEnable,
                        locales = aggregatorGame.locales,
                        platforms = aggregatorGame.platforms.map { Platform.valueOf(it) },
                        playLines = aggregatorGame.playLines
                    )
                )
            }

            // Find or create game
            val gameIdentity = aggregatorGame.symbol.lowercase().replace(" ", "-")
            var game = gameRepository.findByIdentity(gameIdentity)

            if (game == null) {
                game = gameRepository.save(
                    Game(
                        id = UUID.randomUUID(),
                        identity = gameIdentity,
                        name = aggregatorGame.name,
                        providerId = provider.id,
                        images = ImageMap.EMPTY
                    )
                )
            }

            // Link variant to game
            if (variant.gameId == null) {
                gameVariantRepository.linkToGame(variant.id, game.id)
            }

            gameCount++
        }

        return Result.success(SyncGameResult(gameCount, providerNames.size))
    }
}
