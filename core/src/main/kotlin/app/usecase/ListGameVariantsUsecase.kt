package app.usecase

import domain.game.mapper.toGame
import domain.game.mapper.toGameVariant
import domain.provider.mapper.toProvider
import domain.game.model.Game
import domain.game.model.GameVariant
import domain.provider.model.Provider
import domain.game.table.GameTable
import domain.game.table.GameVariantTable
import domain.provider.table.ProviderTable
import core.db.paging
import domain.aggregator.model.Aggregator
import core.model.Page
import core.model.Pageable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class ListGameVariantsUsecase {

    suspend operator fun invoke(pageable: Pageable, filterBuilder: Filter.() -> Unit): Page<GameVariantDto> =
        newSuspendedTransaction {
            val filter = Filter().apply(filterBuilder)

            fun Query.applyFilters() = apply {
                andWhere {
                    val query = "%${filter.query}%"
                    (GameVariantTable.name like query) or
                            (GameVariantTable.providerName like query) or
                            (GameVariantTable.symbol like query)
                }

                filter.aggregator?.also { providerConfigType ->
                    andWhere { GameVariantTable.aggregator eq providerConfigType }
                }

                filter.gameIdentity?.also { gameIdentity ->
                    andWhere { GameTable.identity eq gameIdentity }
                }
            }

            GameVariantTable
                .leftJoin(GameTable, onColumn = { GameVariantTable.gameId }, otherColumn = { GameTable.id })
                .leftJoin(ProviderTable, onColumn = { ProviderTable.id }, otherColumn = { GameTable.providerId })
                .selectAll()
                .applyFilters()
                .paging(pageable)
                .map { resultRow ->
                    val gameVariant = resultRow.toGameVariant()
                    val game = resultRow.toGame()
                    val provider = resultRow.toProvider()

                    GameVariantDto(gameVariant, GameVariantDto.GameResult(game, provider))
                }
        }

    class Filter {
        var query: String = ""
            private set

        var aggregator: Aggregator? = null
            private set

        var gameIdentity: String? = null

        fun withQuery(query: String) = apply { this.query = query }

        fun withAggregator(aggregator: Aggregator) = apply { this.aggregator = aggregator }

        fun withGameIdentity(gameIdentity: String?) = apply { this.gameIdentity = gameIdentity }
    }

    data class GameVariantDto(
        val gameVariant: GameVariant,

        val game: GameResult,
    ) {
        data class GameResult(
            val game: Game,

            val provider: Provider
        )
    }
}