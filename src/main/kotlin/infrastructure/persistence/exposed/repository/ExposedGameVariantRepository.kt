package infrastructure.persistence.exposed.repository

import application.port.outbound.GameVariantRepository
import domain.game.model.GameVariant
import infrastructure.persistence.exposed.table.GameVariantTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsertReturning

/**
 * Exposed implementation of GameVariantRepository.
 */
class ExposedGameVariantRepository : GameVariantRepository {

    override suspend fun saveAll(variants: List<GameVariant>): List<GameVariant> =
        newSuspendedTransaction {
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

                variant.copy(
                    id = row[GameVariantTable.id].value,
                    gameId = row[GameVariantTable.gameId]?.value
                )
            }
        }
}
