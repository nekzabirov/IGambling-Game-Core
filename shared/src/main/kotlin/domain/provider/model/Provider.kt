package domain.provider.model

import core.serializer.UUIDSerializer
import core.value.ImageMap
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Provider(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,

    val identity: String,

    val name: String,

    val images: ImageMap,

    val order: Int = 100,

    @Serializable(with = UUIDSerializer::class)
    val aggregatorId: UUID? = null,

    val active: Boolean = true
)
