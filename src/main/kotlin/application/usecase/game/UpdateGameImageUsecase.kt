package application.usecase.game

import application.port.outbound.FileAdapter
import application.port.outbound.MediaFile
import domain.common.error.NotFoundError
import domain.game.model.Game
import domain.game.repository.GameRepository
import shared.value.ImageMap

/**
 * Command for updating a game image.
 */
data class UpdateGameImageCommand(
    val identity: String,
    val key: String,
    val mediaFile: MediaFile
)

/**
 * Use case for updating a game image.
 * Uploads the file via FileAdapter and sets game.images[key] = path
 */
class UpdateGameImageUsecase(
    private val gameRepository: GameRepository,
    private val fileAdapter: FileAdapter
) {
    suspend operator fun invoke(command: UpdateGameImageCommand): Result<Game> {
        val game = gameRepository.findByIdentity(command.identity)
            ?: return Result.failure(NotFoundError("Game", command.identity))

        val uploadResult = fileAdapter.upload(
            folder = "games/${game.identity}",
            fileName = command.key,
            file = command.mediaFile
        )

        val path = uploadResult.getOrElse {
            return Result.failure(it)
        }

        val updatedImages = game.images.data.toMutableMap()
        updatedImages[command.key] = path

        val updatedGame = game.copy(images = ImageMap(updatedImages))

        return try {
            val saved = gameRepository.update(updatedGame)
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convenience method with individual parameters.
     */
    suspend operator fun invoke(
        identity: String,
        key: String,
        mediaFile: MediaFile
    ): Result<Game> = invoke(UpdateGameImageCommand(identity, key, mediaFile))
}
