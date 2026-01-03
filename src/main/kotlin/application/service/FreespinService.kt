package application.service

import application.port.outbound.AggregatorAdapterRegistry
import domain.common.error.AggregatorNotSupportedError
import domain.common.error.InvalidPresetError
import shared.value.Currency
import kotlinx.datetime.LocalDateTime

/**
 * Result of getting preset.
 */
data class GetPresetResult(
    val preset: Map<String, Any?>
)

/**
 * Application service for freespin operations.
 * Encapsulates aggregator adapter interactions for freespin functionality.
 */
class FreespinService(
    private val gameService: GameService,
    private val aggregatorRegistry: AggregatorAdapterRegistry
) {

    /**
     * Get freespin preset for a game.
     *
     * @param gameIdentity The identity of the game
     * @return Result containing the preset configuration
     */
    suspend fun getPreset(gameIdentity: String): Result<GetPresetResult> {
        val game = gameService.findByIdentity(gameIdentity).getOrElse {
            return Result.failure(it)
        }

        val factory = aggregatorRegistry.getFactory(game.aggregator.aggregator)
            ?: return Result.failure(AggregatorNotSupportedError(game.aggregator.aggregator.name))

        val freespinAdapter = factory.createFreespinAdapter(game.aggregator)

        val preset = freespinAdapter.getPreset(game.symbol).getOrElse {
            return Result.failure(it)
        }

        return Result.success(GetPresetResult(preset))
    }

    /**
     * Create a freespin.
     *
     * @param presetValue The preset configuration values
     * @param referenceId External reference ID for the freespin
     * @param playerId Player ID
     * @param gameIdentity Game identity
     * @param currency Currency for the freespin
     * @param startAt Start time of the freespin validity
     * @param endAt End time of the freespin validity
     * @return Result indicating success or failure
     */
    suspend fun create(
        presetValue: Map<String, Int>,
        referenceId: String,
        playerId: String,
        gameIdentity: String,
        currency: Currency,
        startAt: LocalDateTime,
        endAt: LocalDateTime
    ): Result<Unit> {
        val game = gameService.findByIdentity(gameIdentity).getOrElse {
            return Result.failure(it)
        }

        if (!game.freeSpinEnable) {
            return Result.failure(
                InvalidPresetError(gameIdentity, "Free spins not enabled for this game")
            )
        }

        val factory = aggregatorRegistry.getFactory(game.aggregator.aggregator)
            ?: return Result.failure(AggregatorNotSupportedError(game.aggregator.aggregator.name))

        val freespinAdapter = factory.createFreespinAdapter(game.aggregator)

        return freespinAdapter.createFreespin(
            presetValue = presetValue,
            referenceId = referenceId,
            playerId = playerId,
            gameSymbol = game.symbol,
            currency = currency,
            startAt = startAt,
            endAt = endAt
        )
    }

    /**
     * Cancel a freespin.
     *
     * @param referenceId External reference ID of the freespin to cancel
     * @param gameIdentity Game identity
     * @return Result indicating success or failure
     */
    suspend fun cancel(referenceId: String, gameIdentity: String): Result<Unit> {
        val game = gameService.findByIdentity(gameIdentity).getOrElse {
            return Result.failure(it)
        }

        val factory = aggregatorRegistry.getFactory(game.aggregator.aggregator)
            ?: return Result.failure(AggregatorNotSupportedError(game.aggregator.aggregator.name))

        val freespinAdapter = factory.createFreespinAdapter(game.aggregator)

        return freespinAdapter.cancelFreespin(referenceId = referenceId)
    }
}
