package domain.game.dao

import domain.aggregator.table.AggregatorInfoTable
import domain.game.mapper.toGame
import domain.game.table.GameTable
import domain.game.table.GameVariantTable
import domain.provider.table.ProviderTable
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll

fun GameTable.full() = this
    .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
    .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
    .innerJoin(
        GameVariantTable,
        { GameVariantTable.gameId },
        { GameTable.id },
        { GameVariantTable.aggregator eq AggregatorInfoTable.aggregator })
    .selectAll()

fun GameTable.findBySymbol(symbol: String) = this
    .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
    .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
    .innerJoin(
        GameVariantTable,
        { GameVariantTable.gameId },
        { GameTable.id },
        { GameVariantTable.aggregator eq AggregatorInfoTable.aggregator })
    .selectAll()
    .where { GameVariantTable.symbol eq symbol }
    .singleOrNull()?.toGame()