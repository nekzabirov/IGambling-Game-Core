package com.nekgamebling.domain.session.repository

import com.nekgamebling.domain.session.model.Session
import java.util.UUID

/**
 * Repository interface for Session entity operations.
 */
interface SessionRepository {
    suspend fun findById(id: UUID): Session?
    suspend fun findByToken(token: String): Session?
    suspend fun save(session: Session): Session
    suspend fun delete(id: UUID): Boolean
}
