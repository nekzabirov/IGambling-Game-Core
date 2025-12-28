package application.saga.spin.place.step

import application.saga.ValidationStep
import application.saga.spin.place.PlaceSpinContext
import application.service.AggregatorService
import application.service.GameService
import domain.common.error.GameUnavailableError

/**
 * Step 1: Validate aggregator and game.
 */
class ValidateGameStep(
    private val aggregatorService: AggregatorService,
    private val gameService: GameService
) : ValidationStep<PlaceSpinContext>("validate_game", "Validate Game") {

    override suspend fun execute(context: PlaceSpinContext): Result<Unit> {
        val aggregator = aggregatorService.findById(context.session.aggregatorId).getOrElse {
            return Result.failure(it)
        }

        val game = gameService.findBySymbol(
            symbol = context.gameSymbol,
            aggregator = aggregator.aggregator
        ).getOrElse {
            return Result.failure(it)
        }

        if (!game.isPlayable()) {
            return Result.failure(GameUnavailableError(context.gameSymbol))
        }

        context.game = game
        return Result.success(Unit)
    }
}
