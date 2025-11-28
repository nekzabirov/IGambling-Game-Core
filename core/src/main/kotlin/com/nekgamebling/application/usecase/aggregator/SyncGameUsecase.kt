package com.nekgamebling.application.usecase.aggregator

import com.nekgamebling.application.port.inbound.GamePort
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
import com.nekgamebling.shared.extension.toUrlSlug
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
    private val aggregatorRegistry: AggregatorAdapterRegistry,
    private val gamePort: GamePort
) {
    suspend operator fun invoke(aggregatorIdentity: String): Result<SyncGameResult> {
        val aggregatorInfo = aggregatorRepository.findByIdentity(aggregatorIdentity)
            ?: return Result.failure(NotFoundError("Aggregator", aggregatorIdentity))

        val factory = aggregatorRegistry.getFactory(aggregatorInfo.aggregator)
            ?: return Result.failure(AggregatorNotSupportedError(aggregatorInfo.aggregator.name))

        val gameSyncAdapter = factory.createGameSyncAdapter(aggregatorInfo)

        val games = gameSyncAdapter.listGames().getOrElse {
            return Result.failure(it)
        }

        val variants = games
            .map { aggregatorGame ->
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
                    platforms = aggregatorGame.platforms,
                    playLines = aggregatorGame.playLines
                )
            }
            .let { gameVariantRepository.saveAll(it) }

        gamePort.syncGame(variants, aggregatorInfo)

        val gameCount = variants.size
        val providerNames = variants.map { it.providerName }.distinct()

        return Result.success(SyncGameResult(gameCount, providerNames.size))
    }
}
