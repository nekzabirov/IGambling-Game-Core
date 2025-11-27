package com.nekgamebling.application.usecase.collection

import com.nekgamebling.domain.collection.model.Collection
import com.nekgamebling.domain.collection.repository.CollectionRepository
import com.nekgamebling.domain.common.error.NotFoundError
import com.nekgamebling.shared.value.ImageMap
import com.nekgamebling.shared.value.LocaleName

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
        active: Boolean? = null,
        images: ImageMap? = null
    ): Result<Collection> {
        val existing = collectionRepository.findByIdentity(identity)
            ?: return Result.failure(NotFoundError("Collection", identity))

        val updated = existing.copy(
            name = name ?: existing.name,
            order = order ?: existing.order,
            active = active ?: existing.active,
            images = images ?: existing.images
        )

        return try {
            val saved = collectionRepository.update(updated)
            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
