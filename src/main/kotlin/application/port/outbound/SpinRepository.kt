package application.port.outbound

import domain.common.value.SpinType
import domain.session.model.Spin
import java.util.UUID

/**
 * Port interface for Spin entity persistence operations.
 * Used by saga steps for spin management.
 */
interface SpinRepository {
    /**
     * Find spin by round ID and type.
     */
    suspend fun findByRoundAndType(roundId: UUID, type: SpinType): Spin?

    /**
     * Find all spins for a round.
     */
    suspend fun findAllByRound(roundId: UUID): List<Spin>

    /**
     * Save a new spin and return with generated ID.
     */
    suspend fun save(spin: Spin): Spin
}
