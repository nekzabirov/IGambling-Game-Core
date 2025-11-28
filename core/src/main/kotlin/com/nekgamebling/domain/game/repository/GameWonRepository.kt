package com.nekgamebling.domain.game.repository

import java.util.UUID

/**
 * Repository interface for game wins.
 */
interface GameWonRepository {
    suspend fun save(gameId: UUID, playerId: String, amount: Int, currency: String): Boolean
}
