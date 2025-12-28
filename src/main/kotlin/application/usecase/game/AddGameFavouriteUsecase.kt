package application.usecase.game

import domain.common.event.GameFavouriteAddedEvent
import application.port.outbound.EventPublisherAdapter
import domain.common.error.NotFoundError
import domain.game.repository.GameFavouriteRepository
import domain.game.repository.GameRepository

/**
 * Use case for adding a game to player's favorites.
 */
class AddGameFavouriteUsecase(
    private val gameRepository: GameRepository,
    private val favouriteRepository: GameFavouriteRepository,
    private val eventPublisher: EventPublisherAdapter
) {
    suspend operator fun invoke(gameIdentity: String, playerId: String): Result<Unit> {
        // Verify game exists
        val game = gameRepository.findByIdentity(gameIdentity)
            ?: return Result.failure(NotFoundError("Game", gameIdentity))

        // Add to favorites
        favouriteRepository.add(playerId, game.id)

        // Publish event
        eventPublisher.publish(
            GameFavouriteAddedEvent(
                gameId = game.id.toString(),
                gameIdentity = game.identity,
                playerId = playerId
            )
        )

        return Result.success(Unit)
    }
}
