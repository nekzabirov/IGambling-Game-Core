package application.service

import application.port.outbound.CacheAdapter
import domain.common.error.NotFoundError
import domain.common.error.SessionInvalidError
import domain.session.model.Session
import domain.session.repository.SessionRepository
import shared.value.SessionToken
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

/**
 * Application service for session-related operations.
 * Uses constructor injection for all dependencies.
 */
class SessionService(
    private val sessionRepository: SessionRepository,
    private val cacheAdapter: CacheAdapter
) {
    private val secureRandom = SecureRandom()

    /**
     * Generate a secure session token.
     */
    fun generateSessionToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Find session by token.
     */
    suspend fun findByToken(token: SessionToken): Result<Session> {
        val cacheKey = "$CACHE_PREFIX:token=$token"

        cacheAdapter.get<Session>(cacheKey)?.let {
            return Result.success(it)
        }

        val session = sessionRepository.findByToken(token.value)
            ?: return Result.failure(SessionInvalidError(token.value))

        cacheAdapter.save(cacheKey, session, CACHE_TTL)

        return Result.success(session)
    }

    /**
     * Find session by ID.
     */
    suspend fun findById(id: UUID): Result<Session> {
        val session = sessionRepository.findById(id)
            ?: return Result.failure(NotFoundError("Session", id.toString()))

        return Result.success(session)
    }

    /**
     * Create a new session.
     */
    suspend fun createSession(session: Session): Result<Session> {
        val savedSession = sessionRepository.save(session)
        return Result.success(savedSession)
    }

    companion object {
        private val CACHE_TTL = 5.minutes
        private const val CACHE_PREFIX = "session:"
    }
}
