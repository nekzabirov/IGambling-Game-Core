package application.port.outbound

import domain.common.value.SpinType
import domain.session.model.Round
import domain.session.model.Spin
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
     * Find round with its place spin in a single query (optimized for settle/rollback).
     * Returns null if round doesn't exist.
     */
    suspend fun findWithPlaceSpin(sessionId: UUID, extId: String): Pair<Round, Spin>? =
        findBySessionAndExtId(sessionId, extId)?.let { round ->
            // Default implementation - can be overridden for single-query optimization
            null
        }

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
