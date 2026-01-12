package application.port.outbound

/**
 * Port interface for player operations.
 * Implementations connect to external player service.
 */
interface PlayerAdapter {
    /**
     * Get player's current bet limit.
     * Returns null if no limit is set.
     */
    suspend fun findCurrentBetLimit(playerId: String): Result<Long?>
}
