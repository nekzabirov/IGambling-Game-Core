package application.usecase.collection

import domain.collection.model.Collection
import domain.collection.repository.CollectionRepository
import domain.common.error.NotFoundError
import shared.value.LocaleName

/**
 * Use case for updating a collection.
 */
class UpdateCollectionUsecase(
    private val collectionRepository: CollectionRepository
) {
    suspend operator fun invoke(
        identity: String,
        name: LocaleName? = null,
        order: Int? = null,
        active: Boolean? = null
    ): Result<Collection> {
        val existing = collectionRepository.findByIdentity(identity)
            ?: return Result.failure(NotFoundError("Collection", identity))

        val updated = existing.copy(
            name = name ?: existing.name,
            order = order ?: existing.order,
            active = active ?: existing.active
        )

        return try {
            val saved = collectionRepository.update(updated)
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
