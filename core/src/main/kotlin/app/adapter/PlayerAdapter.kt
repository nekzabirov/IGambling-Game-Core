package app.adapter

interface PlayerAdapter {
    suspend fun findCurrentBetLimit(playerId: String): Result<Int?>
}