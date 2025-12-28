package application.service

import application.port.outbound.CacheAdapter
import domain.common.error.NotFoundError
import domain.common.error.SessionInvalidError
import domain.session.model.Session
import domain.session.repository.SessionRepository
import infrastructure.persistence.cache.CachingRepository
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
    cacheAdapter: CacheAdapter
) {
    companion object {
        private val CACHE_TTL = 5.minutes
        private const val CACHE_PREFIX_TOKEN = "session:token:"
        private const val CACHE_PREFIX_ID = "session:id:"
    }

    private val secureRandom = SecureRandom()

    private val tokenCache = CachingRepository<Session>(
        cacheAdapter = cacheAdapter,
        cachePrefix = CACHE_PREFIX_TOKEN,
        ttl = CACHE_TTL
    )

    private val idCache = CachingRepository<Session>(
        cacheAdapter = cacheAdapter,
        cachePrefix = CACHE_PREFIX_ID,
        ttl = CACHE_TTL
    )

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
    suspend fun findByToken(token: SessionToken): Result<Session> =
        tokenCache.getOrLoadResult(
            key = token.value,
            notFoundError = { SessionInvalidError(token.value) },
            loader = { sessionRepository.findByToken(token.value) }
        )

    /**
     * Find session by ID.
     */
    suspend fun findById(id: UUID): Result<Session> =
        idCache.getOrLoadResult(
            key = id.toString(),
            notFoundError = { NotFoundError("Session", id.toString()) },
            loader = { sessionRepository.findById(id) }
        )

    /**
     * Create a new session.
     */
    suspend fun createSession(session: Session): Result<Session> {
        val savedSession = sessionRepository.save(session)
        // Cache the new session
        tokenCache.update(savedSession.token, savedSession)
        idCache.update(savedSession.id.toString(), savedSession)
        return Result.success(savedSession)
    }

    /**
     * Invalidate cache for a session.
     */
    suspend fun invalidateCache(session: Session) {
        tokenCache.invalidate(session.token)
        idCache.invalidate(session.id.toString())
    }
}
