package application.usecase.game

import application.event.GameWonEvent
import application.port.outbound.EventPublisherAdapter
import domain.common.error.NotFoundError
import domain.game.repository.GameRepository
import domain.game.repository.GameWonRepository
import shared.value.Currency

/**
 * Use case for recording a game win.
 */
class AddGameWonUsecase(
    private val gameRepository: GameRepository,
    private val gameWonRepository: GameWonRepository,
    private val eventPublisher: EventPublisherAdapter
) {
    suspend operator fun invoke(
        gameIdentity: String,
        playerId: String,
        amount: Int,
        currency: Currency
    ): Result<Unit> {
        val game = gameRepository.findByIdentity(gameIdentity)
            ?: return Result.failure(NotFoundError("Game", gameIdentity))

        gameWonRepository.save(game.id, playerId, amount, currency.value)

        eventPublisher.publish(
            GameWonEvent(
                gameId = game.id.toString(),
                gameIdentity = game.identity,
                playerId = playerId,
                amount = amount,
                currency = currency
            )
        )

        return Result.success(Unit)
    }
}
