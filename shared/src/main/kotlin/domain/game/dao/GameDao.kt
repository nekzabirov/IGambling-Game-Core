package domain.game.dao

import domain.aggregator.table.AggregatorInfoTable
import domain.game.table.GameTable
import domain.game.table.GameVariantTable
import domain.provider.table.ProviderTable
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll

object GameDao {
    val gameFull = GameTable
        .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
        .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
        .innerJoin(
            GameVariantTable,
            { GameVariantTable.gameId },
            { GameTable.id },
            { GameVariantTable.aggregator eq AggregatorInfoTable.aggregator })
        .selectAll()
}