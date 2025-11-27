package com.nekgamebling.application.usecase.spin

import com.nekgamebling.application.port.outbound.AggregatorAdapterRegistry
import com.nekgamebling.application.service.GameService
import com.nekgamebling.domain.common.error.AggregatorNotSupportedError

/**
 * Result of getting preset.
 */
data class GetPresetResult(
    val preset: Map<String, Any?>
)

/**
 * Use case for getting freespin preset.
 */
class GetPresetUsecase(
    private val gameService: GameService,
    private val aggregatorRegistry: AggregatorAdapterRegistry
) {
    suspend operator fun invoke(gameIdentity: String): Result<GetPresetResult> {
        val game = gameService.findByIdentity(gameIdentity).getOrElse {
            return Result.failure(it)
        }

        val factory = aggregatorRegistry.getFactory(game.aggregator.aggregator)
            ?: return Result.failure(AggregatorNotSupportedError(game.aggregator.aggregator.name))

        val freespinAdapter = factory.createFreespinAdapter(game.aggregator)

        val preset = freespinAdapter.getPreset(game.symbol, game.aggregator.identity).getOrElse {
            return Result.failure(it)
        }

        return Result.success(GetPresetResult(preset))
    }
}
