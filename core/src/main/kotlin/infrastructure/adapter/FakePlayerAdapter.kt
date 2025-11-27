package infrastructure.adapter

import app.adapter.PlayerAdapter

class FakePlayerAdapter : PlayerAdapter {
    override suspend fun findCurrentBetLimit(playerId: String): Result<Int?> {
        return Result.success(null)
    }
}