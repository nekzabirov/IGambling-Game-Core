package infrastructure.adapter

import application.port.outbound.PlayerAdapter

/**
 * Fake player adapter for development/testing.
 * Replace with real implementation in production.
 */
class FakePlayerAdapter : PlayerAdapter {
    private val betLimits = mutableMapOf<String, Int>()

    fun setBetLimit(playerId: String, limit: Int) {
        betLimits[playerId] = limit
    }

    override suspend fun findCurrentBetLimit(playerId: String): Result<Int?> {
        return Result.success(betLimits[playerId])
    }
}
