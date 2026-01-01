package application.usecase.provider

import application.port.outbound.FileAdapter
import application.port.outbound.MediaFile
import domain.common.error.NotFoundError
import domain.provider.model.Provider
import domain.provider.repository.ProviderRepository
import shared.value.ImageMap

/**
 * Command for updating a provider image.
 */
data class UpdateProviderImageCommand(
    val identity: String,
    val key: String,
    val mediaFile: MediaFile
)

/**
 * Use case for updating a provider image.
 * Uploads the file via FileAdapter and sets provider.images[key] = path
 */
class UpdateProviderImageUsecase(
    private val providerRepository: ProviderRepository,
    private val fileAdapter: FileAdapter
) {
    suspend operator fun invoke(command: UpdateProviderImageCommand): Result<Provider> {
        val provider = providerRepository.findByIdentity(command.identity)
            ?: return Result.failure(NotFoundError("Provider", command.identity))

        val uploadResult = fileAdapter.upload(
            folder = "providers/${provider.identity}",
            fileName = command.key,
            file = command.mediaFile
        )

        val path = uploadResult.getOrElse {
            return Result.failure(it)
        }

        val updatedImages = provider.images.data.toMutableMap()
        updatedImages[command.key] = path

        val updatedProvider = provider.copy(images = ImageMap(updatedImages))

        return try {
            val saved = providerRepository.update(updatedProvider)
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
    ): Result<Provider> = invoke(UpdateProviderImageCommand(identity, key, mediaFile))
}
