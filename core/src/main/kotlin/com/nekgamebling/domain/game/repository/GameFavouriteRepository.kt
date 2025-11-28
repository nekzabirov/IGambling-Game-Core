package com.nekgamebling.domain.game.repository

import java.util.UUID

/**
 * Repository interface for game favorites.
 */
interface GameFavouriteRepository {
    suspend fun add(playerId: String, gameId: UUID): Boolean
    suspend fun remove(playerId: String, gameId: UUID): Boolean
    suspend fun exists(playerId: String, gameId: UUID): Boolean
    suspend fun findByPlayer(playerId: String): List<UUID>
}
