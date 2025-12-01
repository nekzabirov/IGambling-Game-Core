package application.usecase.game

import application.event.GameFavouriteRemovedEvent
import application.port.outbound.EventPublisherAdapter
import domain.common.error.NotFoundError
import domain.game.repository.GameFavouriteRepository
import domain.game.repository.GameRepository

/**
 * Use case for removing a game from player's favorites.
 */
class RemoveGameFavouriteUsecase(
    private val gameRepository: GameRepository,
    private val favouriteRepository: GameFavouriteRepository,
    private val eventPublisher: EventPublisherAdapter
) {
    suspend operator fun invoke(gameIdentity: String, playerId: String): Result<Unit> {
        // Verify game exists
        val game = gameRepository.findByIdentity(gameIdentity)
            ?: return Result.failure(NotFoundError("Game", gameIdentity))

        // Remove from favorites
        favouriteRepository.remove(playerId, game.id)

        // Publish event
        eventPublisher.publish(
            GameFavouriteRemovedEvent(
                gameId = game.id.toString(),
                gameIdentity = game.identity,
                playerId = playerId
            )
        )

        return Result.success(Unit)
    }
}
