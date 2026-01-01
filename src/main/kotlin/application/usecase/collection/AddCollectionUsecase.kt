package application.usecase.collection

import domain.collection.model.Collection
import domain.collection.repository.CollectionRepository
import domain.common.error.DuplicateEntityError
import shared.value.LocaleName
import java.util.UUID

/**
 * Use case for adding a new collection.
 */
class AddCollectionUsecase(
    private val collectionRepository: CollectionRepository
) {
    suspend operator fun invoke(identity: String, name: LocaleName): Result<Collection> {
        if (collectionRepository.existsByIdentity(identity)) {
            return Result.failure(DuplicateEntityError("Collection", identity))
        }

        val collection = Collection(
            id = UUID.randomUUID(),
            identity = identity,
            name = name
        )

        return try {
            val saved = collectionRepository.save(collection)
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
