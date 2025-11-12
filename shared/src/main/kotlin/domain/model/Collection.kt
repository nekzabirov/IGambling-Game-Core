package domain.model

import domain.value.ImageMap
import domain.value.LocaleName
import java.util.UUID

data class Collection(
    val id: UUID = UUID.randomUUID(),

    val identity: String,

    val name: LocaleName,

    val active: Boolean = true,

    val order: Int = 100,

    val images: ImageMap,
)
