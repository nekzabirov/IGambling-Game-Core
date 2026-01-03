package infrastructure.persistence.exposed.repository

import domain.game.model.Game
import domain.game.model.GameWithDetails
import domain.game.repository.GameRepository
import infrastructure.persistence.exposed.mapper.*
import infrastructure.persistence.exposed.table.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import domain.common.value.Aggregator
import java.util.UUID

/**
 * Exposed implementation of GameRepository.
 * Provides methods used by GameService for caching with database access.
 */
class ExposedGameRepository : GameRepository {

    override suspend fun findBySymbol(symbol: String, aggregator: Aggregator): Game? = newSuspendedTransaction {
        GameTable
            .innerJoin(
                GameVariantTable,
                { GameTable.id },
                { GameVariantTable.gameId },
                { GameVariantTable.aggregator eq aggregator })
            .selectAll()
            .where { GameVariantTable.symbol eq symbol }
            .singleOrNull()
            ?.toGame()
    }

    override suspend fun findWithDetailsById(id: UUID): GameWithDetails? = newSuspendedTransaction {
        buildFullGameQuery()
            .andWhere { GameTable.id eq id }
            .singleOrNull()
            ?.toGameWithDetails()
    }

    override suspend fun findWithDetailsByIdentity(identity: String): GameWithDetails? = newSuspendedTransaction {
        buildFullGameQuery()
            .andWhere { GameTable.identity eq identity }
            .singleOrNull()
            ?.toGameWithDetails()
    }

    private fun buildFullGameQuery(): Query {
        return GameTable
            .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
            .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
            .innerJoin(GameVariantTable, { GameVariantTable.gameId }, { GameTable.id }) {
                GameVariantTable.aggregator eq AggregatorInfoTable.aggregator
            }
            .selectAll()
            .andWhere { GameTable.active eq true }
            .andWhere { ProviderTable.active eq true }
            .andWhere { AggregatorInfoTable.active eq true }
    }
}
