package infrastructure.aggregator.onegamehub.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameDto(
    val id: String,

    val name: String,

    val brand: String,

    @SerialName("is_free_rounds_supported")
    val freespinEnable: Boolean,

    @SerialName("is_demo_supported")
    val demoEnable: Boolean,

    val paylines: Int = 0
)
