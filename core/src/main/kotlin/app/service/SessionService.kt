package app.service

import domain.session.mapper.toSession
import domain.session.model.Session
import domain.session.table.SessionTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.security.SecureRandom
import java.util.Base64

object SessionService {
    private val secureRandom = SecureRandom()
    private val base64Encoder = Base64.getUrlEncoder().withoutPadding()

    fun generateSessionToken(): String {
        val randomBytes = ByteArray(32) // 32 bytes = 256 bits
        secureRandom.nextBytes(randomBytes)
        return base64Encoder.encodeToString(randomBytes)
    }

    suspend fun findByToken(token: String): Result<Session> = newSuspendedTransaction {
        val session = SessionTable.selectAll()
            .where { SessionTable.token eq token }
            .singleOrNull()?.toSession()
            ?: return@newSuspendedTransaction Result.failure(NotFoundException("Session not found"))

        Result.success(session)
    }
}