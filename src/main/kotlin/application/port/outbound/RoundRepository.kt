package application.port.outbound

import domain.session.model.Round
import java.util.UUID

/**
 * Port interface for Round entity persistence operations.
 * Used by saga steps for round management.
 */
interface RoundRepository {
    /**
     * Find round by session ID and external round ID.
     */
    suspend fun findBySessionAndExtId(sessionId: UUID, extId: String): Round?

    /**
     * Find or create round (atomic upsert operation).
     * Returns the existing or newly created round.
     */
    suspend fun findOrCreate(sessionId: UUID, gameId: UUID, extId: String): Round

    /**
     * Mark round as finished.
     */
    suspend fun finish(roundId: UUID)
}
