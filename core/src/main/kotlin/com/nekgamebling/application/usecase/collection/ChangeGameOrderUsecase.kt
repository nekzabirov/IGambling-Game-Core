package com.nekgamebling.application.usecase.collection

import com.nekgamebling.domain.collection.repository.CollectionRepository
import com.nekgamebling.domain.common.error.NotFoundError
import com.nekgamebling.domain.game.repository.GameRepository

/**
 * Use case for changing game order in a collection.
 */
class ChangeGameOrderUsecase(
    private val collectionRepository: CollectionRepository,
    private val gameRepository: GameRepository
) {
    suspend operator fun invoke(collectionIdentity: String, gameIdentity: String, order: Int): Result<Unit> {
        val collection = collectionRepository.findByIdentity(collectionIdentity)
            ?: return Result.failure(NotFoundError("Collection", collectionIdentity))

        val game = gameRepository.findByIdentity(gameIdentity)
            ?: return Result.failure(NotFoundError("Game", gameIdentity))

        collectionRepository.updateGameOrder(collection.id, game.id, order)
        return Result.success(Unit)
    }
}
