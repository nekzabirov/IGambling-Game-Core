package app.service

import domain.aggregator.table.AggregatorInfoTable
import domain.game.dao.GameDao
import domain.game.mapper.toGameFull
import domain.game.model.GameFull
import domain.game.table.GameTable
import domain.provider.table.ProviderTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

object GameService {
    suspend fun findByIdentity(identity: String): Result<GameFull> = newSuspendedTransaction {
        val gameResult = GameDao.gameFull
            .where { GameTable.identity eq identity and (GameTable.active eq true) and (ProviderTable.active eq true) and (AggregatorInfoTable.active eq true) }
            .singleOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundException("Game not found"))

        Result.success(gameResult.toGameFull())
    }

    suspend fun findById(id: UUID): Result<GameFull> = newSuspendedTransaction {
        val gameResult = GameDao.gameFull
            .where { GameTable.id eq id and (GameTable.active eq true) and (ProviderTable.active eq true) and (AggregatorInfoTable.active eq true) }
            .singleOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundException("Game not found"))

        Result.success(gameResult.toGameFull())
    }
}