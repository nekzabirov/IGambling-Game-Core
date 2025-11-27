package com.nekgamebling.application.usecase.game

import com.nekgamebling.application.event.GameFavouriteRemovedEvent
import com.nekgamebling.application.port.outbound.EventPublisherPort
import com.nekgamebling.domain.common.error.NotFoundError
import com.nekgamebling.domain.game.repository.GameFavouriteRepository
import com.nekgamebling.domain.game.repository.GameRepository

/**
 * Use case for removing a game from player's favorites.
 */
class RemoveGameFavouriteUsecase(
    private val gameRepository: GameRepository,
    private val favouriteRepository: GameFavouriteRepository,
    private val eventPublisher: EventPublisherPort
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
