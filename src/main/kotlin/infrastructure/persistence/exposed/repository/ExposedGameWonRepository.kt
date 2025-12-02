package infrastructure.persistence.exposed.repository

import domain.game.repository.GameWonRepository
import infrastructure.persistence.exposed.table.GameWonTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigInteger
import java.util.UUID

/**
 * Exposed implementation of GameWonRepository.
 */
class ExposedGameWonRepository : GameWonRepository {

    override suspend fun save(gameId: UUID, playerId: String, amount: BigInteger, currency: String): Boolean =
        newSuspendedTransaction {
            GameWonTable.insert {
                it[GameWonTable.gameId] = gameId
                it[GameWonTable.playerId] = playerId
                it[GameWonTable.amount] = amount.toLong()
                it[GameWonTable.currency] = currency
            }
            true
        }
}
