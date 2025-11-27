package app.event

import core.serializer.UUIDSerializer
import domain.game.model.GameFull
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SessionOpenEvent(
    val game: GameFull,

    val playerId: String,

    @Serializable(with = UUIDSerializer::class)
    val sessionId: UUID
) : IEvent {
    override val key: String = "session.open"
}
