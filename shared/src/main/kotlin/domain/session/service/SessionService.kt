package domain.session.service

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
}