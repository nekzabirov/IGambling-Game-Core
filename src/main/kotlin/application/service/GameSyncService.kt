package application.service

import application.port.outbound.AggregatorAdapterRegistry
import application.port.outbound.GameSyncAdapter
import domain.common.error.AggregatorNotSupportedError
import domain.common.error.NotFoundError
import domain.game.model.GameVariant
import infrastructure.persistence.exposed.mapper.toAggregatorInfo
import infrastructure.persistence.exposed.mapper.toGameVariant
import infrastructure.persistence.exposed.table.AggregatorInfoTable
import infrastructure.persistence.exposed.table.GameVariantTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsertReturning
import java.util.UUID

/**
 * Result of game sync operation.
 */
data class SyncGameResult(
    val gameCount: Int,
    val providerCount: Int
)

/**
 * Application service for syncing games from aggregators.
 * Handles the orchestration of fetching games from aggregator APIs and saving them locally.
 */
class GameSyncService(
    private val aggregatorRegistry: AggregatorAdapterRegistry,
    private val gameSyncAdapter: GameSyncAdapter
) {

    /**
     * Sync games from an aggregator.
     *
     * @param aggregatorIdentity The identity of the aggregator to sync from
     * @return Result containing the count of synced games and providers
     */
    suspend fun sync(aggregatorIdentity: String): Result<SyncGameResult> {
        val aggregatorInfo = newSuspendedTransaction {
            AggregatorInfoTable.selectAll()
                .where { AggregatorInfoTable.identity eq aggregatorIdentity }
                .singleOrNull()
                ?.toAggregatorInfo()
        } ?: return Result.failure(NotFoundError("Aggregator", aggregatorIdentity))

        val factory = aggregatorRegistry.getFactory(aggregatorInfo.aggregator)
            ?: return Result.failure(AggregatorNotSupportedError(aggregatorInfo.aggregator.name))

        val aggregatorAdapter = factory.createGameSyncAdapter(aggregatorInfo)

        val games = aggregatorAdapter.listGames().getOrElse {
            return Result.failure(it)
        }

        val variants = games
            .map { aggregatorGame ->
                GameVariant(
                    id = UUID.randomUUID(),
                    symbol = aggregatorGame.symbol,
                    name = aggregatorGame.name,
                    providerName = aggregatorGame.providerName,
                    aggregator = aggregatorInfo.aggregator,
                    freeSpinEnable = aggregatorGame.freeSpinEnable,
                    freeChipEnable = aggregatorGame.freeChipEnable,
                    jackpotEnable = aggregatorGame.jackpotEnable,
                    demoEnable = aggregatorGame.demoEnable,
                    bonusBuyEnable = aggregatorGame.bonusBuyEnable,
                    locales = aggregatorGame.locales,
                    platforms = aggregatorGame.platforms,
                    playLines = aggregatorGame.playLines
                )
            }
            .let { variantsList ->
                saveAllVariants(variantsList)
            }

        gameSyncAdapter.syncGame(variants, aggregatorInfo)

        val gameCount = variants.size
        val providerNames = variants.map { it.providerName }.distinct()

        return Result.success(SyncGameResult(gameCount, providerNames.size))
    }

    private suspend fun saveAllVariants(variants: List<GameVariant>): List<GameVariant> = newSuspendedTransaction {
        variants.map { variant ->
            val row = GameVariantTable.upsertReturning(
                keys = arrayOf(GameVariantTable.symbol, GameVariantTable.aggregator),
                onUpdateExclude = listOf(GameVariantTable.createdAt, GameVariantTable.gameId),
            ) {
                it[gameId] = variant.gameId
                it[symbol] = variant.symbol
                it[name] = variant.name
                it[providerName] = variant.providerName
                it[aggregator] = variant.aggregator
                it[freeSpinEnable] = variant.freeSpinEnable
                it[freeChipEnable] = variant.freeChipEnable
                it[jackpotEnable] = variant.jackpotEnable
                it[demoEnable] = variant.demoEnable
                it[bonusBuyEnable] = variant.bonusBuyEnable
                it[locales] = variant.locales.map { l -> l.value }
                it[platforms] = variant.platforms.map { p -> p.name }
                it[playLines] = variant.playLines
            }.single()

            variant.copy(id = row[GameVariantTable.id].value, gameId = row[GameVariantTable.gameId]?.value)
        }
    }
}
