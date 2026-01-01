package application.usecase.game

import domain.common.error.NotFoundError
import domain.game.model.Game
import domain.game.repository.GameRepository

/**
 * Command for updating a game.
 */
data class UpdateGameCommand(
    val identity: String,
    val name: String? = null,
    val bonusBetEnable: Boolean? = null,
    val bonusWageringEnable: Boolean? = null,
    val active: Boolean? = null
)

/**
 * Use case for updating game properties.
 */
class UpdateGameUsecase(
    private val gameRepository: GameRepository
) {
    suspend operator fun invoke(command: UpdateGameCommand): Result<Game> {
        val existingGame = gameRepository.findByIdentity(command.identity)
            ?: return Result.failure(NotFoundError("Game", command.identity))

        val updatedGame = existingGame.copy(
            name = command.name ?: existingGame.name,
            bonusBetEnable = command.bonusBetEnable ?: existingGame.bonusBetEnable,
            bonusWageringEnable = command.bonusWageringEnable ?: existingGame.bonusWageringEnable,
            active = command.active ?: existingGame.active
        )

        return try {
            val saved = gameRepository.update(updatedGame)
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convenience method for simple updates.
     */
    suspend operator fun invoke(
        identity: String,
        active: Boolean? = null,
        bonusBet: Boolean? = null,
        bonusWagering: Boolean? = null
    ): Result<Game> = invoke(
        UpdateGameCommand(
            identity = identity,
            active = active,
            bonusBetEnable = bonusBet,
            bonusWageringEnable = bonusWagering
        )
    )
}

/**
 * Use case for adding a tag to a game.
 */
class AddGameTagUsecase(
    private val gameRepository: GameRepository
) {
    suspend operator fun invoke(identity: String, tag: String): Result<Unit> {
        val game = gameRepository.findByIdentity(identity)
            ?: return Result.failure(NotFoundError("Game", identity))

        if (!gameRepository.addTag(game.id, tag)) {
            return Result.failure(NotFoundError("Game", identity))
        }
        return Result.success(Unit)
    }
}

/**
 * Use case for removing a tag from a game.
 */
class RemoveGameTagUsecase(
    private val gameRepository: GameRepository
) {
    suspend operator fun invoke(identity: String, tag: String): Result<Unit> {
        val game = gameRepository.findByIdentity(identity)
            ?: return Result.failure(NotFoundError("Game", identity))

        if (!gameRepository.removeTag(game.id, tag)) {
            return Result.failure(NotFoundError("Game", identity))
        }
        return Result.success(Unit)
    }
}
