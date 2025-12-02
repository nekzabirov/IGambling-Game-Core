package domain.game.repository

import java.math.BigInteger
import java.util.UUID

/**
 * Repository interface for game wins.
 */
interface GameWonRepository {
    suspend fun save(gameId: UUID, playerId: String, amount: BigInteger, currency: String): Boolean
}
