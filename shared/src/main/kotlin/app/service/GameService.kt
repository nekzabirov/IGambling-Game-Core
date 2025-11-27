package app.service

import app.adapter.CacheAdapter
import core.error.GameUnavailableError
import domain.aggregator.table.AggregatorInfoTable
import domain.game.dao.findBySymbol
import domain.game.dao.full
import domain.game.mapper.toGameFull
import domain.game.model.Game
import domain.game.model.GameFull
import domain.game.table.GameTable
import domain.provider.table.ProviderTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.component.KoinComponent
import java.util.UUID

object GameService : KoinComponent {
    private val cacheAdapter = getKoin().get<CacheAdapter>()

    suspend fun findByIdentity(identity: String): Result<GameFull> = newSuspendedTransaction {
        val gameResult = GameTable.full()
            .andWhere { GameTable.identity eq identity and (GameTable.active eq true) and (ProviderTable.active eq true) and (AggregatorInfoTable.active eq true) }
            .singleOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundException("Game not found"))

        Result.success(gameResult.toGameFull())
    }

    suspend fun findById(id: UUID): Result<GameFull> = newSuspendedTransaction {
        val gameResult = GameTable.full()
            .andWhere { GameTable.id eq id and (GameTable.active eq true) and (ProviderTable.active eq true) and (AggregatorInfoTable.active eq true) }
            .singleOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundException("Game not found"))

        Result.success(gameResult.toGameFull())
    }

    suspend fun findBySymbol(symbol: String): Result<Game> {
        cacheAdapter.get<Game>("game:$symbol")?.let {
            return Result.success(it)
        }

        return newSuspendedTransaction {
            val game = GameTable.findBySymbol(symbol)
                ?: return@newSuspendedTransaction Result.failure(GameUnavailableError())

            cacheAdapter.save("game:$symbol", game)

            Result.success(game)
        }
    }
}