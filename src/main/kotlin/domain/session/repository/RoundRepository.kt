package domain.session.repository

import domain.session.model.Round
import domain.session.model.RoundDetails
import shared.value.Page
import shared.value.Pageable
import java.util.UUID

/**
 * Repository interface for Round entity operations.
 */
interface RoundRepository {
    suspend fun findById(id: UUID): Round?
    suspend fun findByExtId(sessionId: UUID, extId: String): Round?
    suspend fun findBySessionId(sessionId: UUID): List<Round>
    suspend fun save(round: Round): Round
    suspend fun update(round: Round): Round
    suspend fun finish(id: UUID): Boolean

    /**
     * Find or create a round for the given session and external ID.
     */
    suspend fun findOrCreate(sessionId: UUID, gameId: UUID, extId: String): Round

    /**
     * Find rounds with details (aggregated amounts, game info) with filtering and pagination.
     */
    suspend fun findAllWithDetails(pageable: Pageable, filter: RoundFilter): Page<RoundDetails>
}
