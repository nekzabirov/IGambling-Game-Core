package com.nekgamebling.domain.session.repository

import com.nekgamebling.domain.session.model.Spin
import com.nekgamebling.shared.value.SpinType
import java.util.UUID

/**
 * Repository interface for Spin entity operations.
 */
interface SpinRepository {
    suspend fun findById(id: UUID): Spin?
    suspend fun findByRoundId(roundId: UUID): List<Spin>
    suspend fun findByRoundIdAndType(roundId: UUID, type: SpinType): Spin?
    suspend fun findByExtId(extId: String): Spin?
    suspend fun save(spin: Spin): Spin

    /**
     * Find place spin for a round.
     */
    suspend fun findPlaceSpinByRoundId(roundId: UUID): Spin?
}
