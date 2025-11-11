package com.nekzabirov.catalog.usecase

import com.djmhub.game.shared.domain.core.ext.toUrlSlug
import com.nekzabirov.aggregators.adapter.AggregatorFabric
import com.nekzabirov.aggregators.value.Aggregator
import com.nekzabirov.catalog.mapper.toAggregatorModel
import com.nekzabirov.catalog.mapper.toGame
import com.nekzabirov.catalog.mapper.toGameVariant
import com.nekzabirov.catalog.mapper.toProvider
import com.nekzabirov.catalog.model.AggregatorInfo
import com.nekzabirov.catalog.model.Game
import com.nekzabirov.catalog.model.GameVariant
import com.nekzabirov.catalog.model.Provider
import com.nekzabirov.catalog.table.AggregatorInfoTable
import com.nekzabirov.catalog.table.GameTable
import com.nekzabirov.catalog.table.GameVariantTable
import com.nekzabirov.catalog.table.ProviderTable
import io.ktor.server.plugins.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsertReturning

class SyncGameUsecase {

    private typealias AggregatorGame = com.nekzabirov.aggregators.model.Game

    suspend operator fun invoke(aggregatorIdentity: String): Result<Details> {
        val aggregator = newSuspendedTransaction {
            AggregatorInfoTable.selectAll()
                .where { AggregatorInfoTable.identity eq aggregatorIdentity }
                .singleOrNull()?.toAggregatorModel()
        } ?: return Result.failure(IllegalArgumentException("Aggregator identity not found"))

        val adapter = AggregatorFabric.createAdapter(aggregator.config, aggregator.aggregator)

        val games = adapter.listGames()
            .getOrElse {
                return Result.failure(it)
            }
            .let { saveGameVariants(aggregator.aggregator, it) }

        for (variant in games) {
            if (variant.gameId != null) continue

            val provider = saveProvider(aggregator, variant.providerName)

            val game = saveGame(provider, variant.name)

            newSuspendedTransaction {
                GameVariantTable.update(where = { GameVariantTable.id eq variant.id }) {
                    it[gameId] = game.id
                }
            }
        }

        return Result.success(Details(gameCount = games.size))
    }

    private suspend fun saveGameVariants(aggregator: Aggregator, games: List<AggregatorGame>): List<GameVariant> =
        newSuspendedTransaction {
            val data = mutableListOf<GameVariant>()

            for (entity in games) {
                val row = GameVariantTable.upsertReturning(
                    keys = arrayOf(GameVariantTable.symbol, GameVariantTable.aggregator),
                    onUpdateExclude = listOf(GameVariantTable.createdAt, GameVariantTable.gameId),
                    body = { statement ->
                        statement[symbol] = entity.symbol

                        statement[name] = entity.name

                        statement[providerName] = entity.providerName

                        statement[GameVariantTable.aggregator] = aggregator

                        statement[playLines] = entity.playLines

                        statement[freeSpinEnable] = entity.freeSpinEnable

                        statement[freeChipEnable] = entity.freeChipEnable

                        statement[jackpotEnable] = entity.jackpotEnable

                        statement[demoEnable] = entity.demoEnable

                        statement[bonusBuyEnable] = entity.bonusBuyEnable

                        statement[locales] = entity.locales.map { it.value }

                        statement[platforms] = entity.platforms.map { it.name }
                    }
                ).single()

                data.add(row.toGameVariant())
            }

            data.toList()
        }

    private suspend fun saveProvider(aggregatorInfo: AggregatorInfo, providerName: String): Provider =
        newSuspendedTransaction {
            val providerResult = ProviderTable.selectAll()
                .where { ProviderTable.name eq providerName }
                .singleOrNull()

            if (providerResult != null)
                return@newSuspendedTransaction providerResult.toProvider()

            ProviderTable.insertReturning {
                it[ProviderTable.identity] = providerName.toUrlSlug()
                it[ProviderTable.name] = providerName
                it[ProviderTable.aggregatorId] = aggregatorInfo.id
            }.single().toProvider()
        }

    private suspend fun saveGame(provider: Provider, gameName: String): Game = newSuspendedTransaction {
        val result = GameTable.selectAll()
            .where { GameTable.name eq gameName and (GameTable.providerId eq provider.id) }
            .singleOrNull()

        if (result != null)
            return@newSuspendedTransaction result.toGame()

        GameTable.insertReturning {
            it[name] = gameName
            it[providerId] = provider.id
            it[identity] = "${provider.name}_$gameName".toUrlSlug()
        }.single().toGame()
    }

    data class Details(val gameCount: Int)
}