package application.usecase.aggregator

import application.port.outbound.GameSyncAdapter
import application.port.outbound.AggregatorAdapterRegistry
import domain.aggregator.repository.AggregatorRepository
import domain.common.error.AggregatorNotSupportedError
import domain.common.error.NotFoundError
import domain.game.model.GameVariant
import domain.game.repository.GameRepository
import domain.game.repository.GameVariantRepository
import domain.provider.repository.ProviderRepository
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
    private val gameSyncAdapter: GameSyncAdapter
) {
    suspend operator fun invoke(aggregatorIdentity: String): Result<SyncGameResult> {
        val aggregatorInfo = aggregatorRepository.findByIdentity(aggregatorIdentity)
            ?: return Result.failure(NotFoundError("Aggregator", aggregatorIdentity))

        val factory = aggregatorRegistry.getFactory(aggregatorInfo.aggregator)
            ?: return Result.failure(AggregatorNotSupportedError(aggregatorInfo.aggregator.name))

        val aggregatorAdapter = factory.createGameSyncAdapter(aggregatorInfo)

        val games = aggregatorAdapter.listGames().getOrElse {
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

        gameSyncAdapter.syncGame(variants, aggregatorInfo)

        val gameCount = variants.size
        val providerNames = variants.map { it.providerName }.distinct()

        return Result.success(SyncGameResult(gameCount, providerNames.size))
    }
}
