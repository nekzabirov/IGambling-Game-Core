package com.nekgamebling.application.usecase.spin

import com.nekgamebling.application.port.outbound.AggregatorAdapterRegistry
import com.nekgamebling.application.service.GameService
import com.nekgamebling.domain.common.error.AggregatorNotSupportedError
import com.nekgamebling.domain.common.error.InvalidPresetError
import com.nekgamebling.shared.value.Currency
import kotlinx.datetime.LocalDateTime

/**
 * Use case for creating a freespin.
 */
class CreateFreespinUsecase(
    private val gameService: GameService,
    private val aggregatorRegistry: AggregatorAdapterRegistry
) {
    suspend operator fun invoke(
        presetValue: Map<String, String>,
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
            aggregatorIdentity = game.aggregator.identity,
            presetValue = presetValue,
            referenceId = referenceId,
            playerId = playerId,
            gameSymbol = game.symbol,
            currency = currency,
            startAt = startAt,
            endAt = endAt
        )
    }
}
