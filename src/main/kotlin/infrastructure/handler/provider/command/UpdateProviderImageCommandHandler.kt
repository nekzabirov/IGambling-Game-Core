package com.nekgamebling.infrastructure.handler.provider.command

import application.port.inbound.CommandHandler
import application.port.outbound.FileAdapter
import application.port.outbound.MediaFile
import com.nekgamebling.application.port.inbound.provider.command.UpdateProviderImageCommand
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.table.ProviderTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import shared.value.ImageMap

class UpdateProviderImageCommandHandler(
    private val fileAdapter: FileAdapter
) : CommandHandler<UpdateProviderImageCommand, Unit> {

    override suspend fun handle(command: UpdateProviderImageCommand): Result<Unit> {
        // Check if provider exists and get current images
        val currentImages = newSuspendedTransaction {
            ProviderTable
                .selectAll()
                .where { ProviderTable.identity eq command.identity }
                .firstOrNull()
                ?.get(ProviderTable.images)
        } ?: return Result.failure(NotFoundError("Provider", command.identity))

        // Upload the file
        val uploadResult = fileAdapter.upload(
            folder = "providers/${command.identity}",
            fileName = command.key,
            file = MediaFile(ext = command.extension, bytes = command.file)
        )

        val imageUrl = uploadResult.getOrElse { error ->
            return Result.failure(error)
        }

        // Update provider images
        val updatedImages = ImageMap(currentImages.data + (command.key to imageUrl))

        newSuspendedTransaction {
            ProviderTable.update({ ProviderTable.identity eq command.identity }) {
                it[images] = updatedImages
            }
        }

        return Result.success(Unit)
    }
}
