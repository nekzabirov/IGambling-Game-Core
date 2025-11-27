package domain.game.model

import core.value.ImageMap
import java.util.UUID

data class Game(
    val id: UUID,

    val identity: String,

    val name: String,

    val providerId: UUID,

    val images: ImageMap = ImageMap(emptyMap()),

    val bonusBetEnable: Boolean = true,

    val bonusWageringEnable: Boolean = true,

    val tags: List<String> = emptyList(),

    val active: Boolean = true
)
