package infrastructure.adapter

import application.port.outbound.PlayerAdapter
import java.math.BigInteger

/**
 * Fake player adapter for development/testing.
 * Replace with real implementation in production.
 */
class FakePlayerAdapter : PlayerAdapter {
    private val betLimits = mutableMapOf<String, BigInteger>()

    fun setBetLimit(playerId: String, limit: BigInteger) {
        betLimits[playerId] = limit
    }

    override suspend fun findCurrentBetLimit(playerId: String): Result<BigInteger?> {
        return Result.success(betLimits[playerId])
    }
}
