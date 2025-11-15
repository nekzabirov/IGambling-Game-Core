package domain.game.service

import core.value.Locale
import core.value.Platform
import domain.aggregator.mapper.toAggregatorModel
import domain.aggregator.table.AggregatorInfoTable
import domain.game.model.GameFull
import domain.game.table.GameTable
import domain.game.table.GameVariantTable
import domain.provider.mapper.toProvider
import domain.provider.table.ProviderTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object GameService {
    suspend fun findByIdentity(identity: String): Result<GameFull> = newSuspendedTransaction {
        val gameResult = GameTable
            .innerJoin(ProviderTable, { ProviderTable.id }, { GameTable.providerId })
            .innerJoin(AggregatorInfoTable, { AggregatorInfoTable.id }, { ProviderTable.aggregatorId })
            .innerJoin(
                GameVariantTable,
                { GameVariantTable.gameId },
                { GameTable.id },
                { GameVariantTable.aggregator eq AggregatorInfoTable.aggregator })
            .selectAll()
            .where { GameTable.identity eq identity and (GameTable.active eq true) and (ProviderTable.active eq true) and (AggregatorInfoTable.active eq true) }
            .singleOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundException("Game not found"))

        GameFull(
            id = gameResult[GameTable.id].value,

            identity = gameResult[GameTable.identity],

            name = gameResult[GameTable.name],

            bonusBetEnable = gameResult[GameTable.bonusBetEnable],

            bonusWageringEnable = gameResult[GameTable.bonusWageringEnable],

            tags = gameResult[GameTable.tags].toList(),

            symbol = gameResult[GameVariantTable.symbol],

            freeSpinEnable = gameResult[GameVariantTable.freeSpinEnable],

            freeChipEnable = gameResult[GameVariantTable.freeChipEnable],

            jackpotEnable = gameResult[GameVariantTable.jackpotEnable],

            demoEnable = gameResult[GameVariantTable.demoEnable],

            bonusBuyEnable = gameResult[GameVariantTable.bonusBuyEnable],

            locales = gameResult[GameVariantTable.locales].toList().map { Locale(it) },

            platforms = gameResult[GameVariantTable.platforms].map { Platform.valueOf(it) },

            playLines = gameResult[GameVariantTable.playLines],

            provider = gameResult.toProvider(),

            aggregator = gameResult.toAggregatorModel(),
        )
            .let { Result.success(it) }
    }
}