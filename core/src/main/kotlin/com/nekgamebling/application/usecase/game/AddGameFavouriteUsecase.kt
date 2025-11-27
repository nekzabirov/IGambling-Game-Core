package com.nekgamebling.application.usecase.game

import com.nekgamebling.application.event.GameFavouriteAddedEvent
import com.nekgamebling.application.port.outbound.EventPublisherPort
import com.nekgamebling.domain.common.error.NotFoundError
import com.nekgamebling.domain.game.repository.GameFavouriteRepository
import com.nekgamebling.domain.game.repository.GameRepository

/**
 * Use case for adding a game to player's favorites.
 */
class AddGameFavouriteUsecase(
    private val gameRepository: GameRepository,
    private val favouriteRepository: GameFavouriteRepository,
    private val eventPublisher: EventPublisherPort
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
