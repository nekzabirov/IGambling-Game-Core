package com.nekgamebling.application.usecase.spin

import com.nekgamebling.application.port.outbound.AggregatorAdapterRegistry
import com.nekgamebling.application.service.GameService
import com.nekgamebling.domain.common.error.AggregatorNotSupportedError

/**
 * Use case for canceling a freespin.
 */
class CancelFreespinUsecase(
    private val gameService: GameService,
    private val aggregatorRegistry: AggregatorAdapterRegistry
) {
    suspend operator fun invoke(referenceId: String, gameIdentity: String): Result<Unit> {
        val game = gameService.findByIdentity(gameIdentity).getOrElse {
            return Result.failure(it)
        }

        val factory = aggregatorRegistry.getFactory(game.aggregator.aggregator)
            ?: return Result.failure(AggregatorNotSupportedError(game.aggregator.aggregator.name))

        val freespinAdapter = factory.createFreespinAdapter(game.aggregator)

        return freespinAdapter.cancelFreespin(
            aggregatorIdentity = game.aggregator.identity,
            referenceId = referenceId,
            gameSymbol = game.symbol
        )
    }
}
