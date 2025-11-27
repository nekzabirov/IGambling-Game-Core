package com.nekgamebling.application.usecase.collection

import com.nekgamebling.domain.collection.model.Collection
import com.nekgamebling.domain.collection.repository.CollectionRepository
import com.nekgamebling.domain.common.error.DuplicateEntityError
import com.nekgamebling.shared.value.ImageMap
import com.nekgamebling.shared.value.LocaleName
import java.util.UUID

/**
 * Use case for adding a new collection.
 */
class AddCollectionUsecase(
    private val collectionRepository: CollectionRepository
) {
    suspend operator fun invoke(identity: String, name: LocaleName, images: ImageMap = ImageMap.EMPTY): Result<Collection> {
        if (collectionRepository.existsByIdentity(identity)) {
            return Result.failure(DuplicateEntityError("Collection", identity))
        }

        val collection = Collection(
            id = UUID.randomUUID(),
            identity = identity,
            name = name,
            images = images
        )

        return try {
            val saved = collectionRepository.save(collection)
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
