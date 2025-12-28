package infrastructure.external.turbo.dto

import kotlinx.serialization.Serializable

@Serializable
data class TurboResponse<T>(
    val data: T? = null
)
