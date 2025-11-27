package app.event

import domain.game.model.GameFull
import java.util.UUID

data class SessionOpenEvent(
    val game: GameFull,

    val playerId: String,

    val sessionId: UUID
) : IEvent {
    override val key: String = "session.open"
}
