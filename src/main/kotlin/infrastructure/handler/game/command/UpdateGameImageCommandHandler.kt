package infrastructure.handler

import application.port.inbound.CommandHandler
import application.port.inbound.command.UpdateGameImageCommand
import application.port.outbound.FileAdapter
import application.port.outbound.MediaFile
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.table.GameTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import shared.value.ImageMap

class UpdateGameImageCommandHandler(
    private val fileAdapter: FileAdapter
) : CommandHandler<UpdateGameImageCommand, Unit> {

    override suspend fun handle(command: UpdateGameImageCommand): Result<Unit> {
        // Check if game exists and get current images
        val currentImages = newSuspendedTransaction {
            GameTable
                .selectAll()
                .where { GameTable.identity eq command.identity }
                .firstOrNull()
                ?.get(GameTable.images)
        } ?: return Result.failure(NotFoundError("Game", command.identity))

        // Upload the file
        val uploadResult = fileAdapter.upload(
            folder = "games/${command.identity}",
            fileName = command.key,
            file = MediaFile(ext = command.extension, bytes = command.file)
        )

        val imageUrl = uploadResult.getOrElse { error ->
            return Result.failure(error)
        }

        // Update game images
        val updatedImages = ImageMap(currentImages.data + (command.key to imageUrl))

        newSuspendedTransaction {
            GameTable.update({ GameTable.identity eq command.identity }) {
                it[images] = updatedImages
            }
        }

        return Result.success(Unit)
    }
}
