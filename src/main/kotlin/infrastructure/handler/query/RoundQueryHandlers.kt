package infrastructure.handler.query

import application.port.inbound.QueryHandler
import application.port.inbound.query.*
import domain.common.value.SpinType
import domain.session.repository.RoundFilter
import infrastructure.persistence.exposed.mapper.toGameWithDetails
import infrastructure.persistence.exposed.table.*
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Currency
import shared.value.Page
import java.math.BigInteger
import java.util.UUID

/**
 * Query handler for getting round details with pagination and filtering.
 * Uses optimized 3-query pattern with aggregation.
 */
class GetRoundsDetailsQueryHandler : QueryHandler<GetRoundsDetailsQuery, Page<RoundDetailsReadModel>> {
    override suspend fun handle(query: GetRoundsDetailsQuery): Page<RoundDetailsReadModel> = newSuspendedTransaction {
        val pageable = query.pageable
        val filter = query.filter

        // Build base query with filters for counting
        val countQuery = buildRoundBaseQuery().applyFilters(filter)
        val totalCount = countQuery.withDistinct().count()

        if (totalCount == 0L) {
            return@newSuspendedTransaction Page.empty()
        }

        val totalPages = pageable.getTotalPages(totalCount)

        // Get paginated round IDs
        val roundIds = buildRoundBaseQuery()
            .applyFilters(filter)
            .withDistinct()
            .orderBy(RoundTable.createdAt to SortOrder.DESC)
            .limit(pageable.sizeReal)
            .offset(pageable.offset)
            .map { it[RoundTable.id].value }

        if (roundIds.isEmpty()) {
            return@newSuspendedTransaction Page.empty()
        }

        // Fetch rounds with game details
        val roundsData = buildFullRoundQuery()
            .andWhere { RoundTable.id inList roundIds }
            .associate { row ->
                val roundId = row[RoundTable.id].value
                roundId to RoundRowData(
                    roundId = roundId,
                    finished = row[RoundTable.finished],
                    createdAt = row[RoundTable.createdAt],
                    finishedAt = row[RoundTable.finishedAt],
                    currency = Currency(row[SessionTable.currency]),
                    gameIdentity = row[GameTable.identity],
                    gameName = row[GameTable.name],
                    providerName = row[ProviderTable.name]
                )
            }

        // Aggregate spin amounts per round
        val spinAmounts = SpinTable
            .select(SpinTable.roundId, SpinTable.type, SpinTable.amount.sum(), SpinTable.freeSpinId)
            .where { SpinTable.roundId inList roundIds }
            .groupBy(SpinTable.roundId, SpinTable.type, SpinTable.freeSpinId)
            .fold(mutableMapOf<UUID, SpinAmounts>()) { acc, row ->
                val roundId = row[SpinTable.roundId]?.value ?: return@fold acc
                val type = row[SpinTable.type]
                val amount = row[SpinTable.amount.sum()] ?: 0L

                val current = acc.getOrPut(roundId) { SpinAmounts() }
                when (type) {
                    SpinType.PLACE -> current.placeAmount += amount.toBigInteger()
                    SpinType.SETTLE -> current.settleAmount += amount.toBigInteger()
                    SpinType.ROLLBACK -> { /* Not included in totals */ }
                }
                val freeSpinId = row[SpinTable.freeSpinId]
                if (freeSpinId != null && current.freeSpinId == null) {
                    current.freeSpinId = freeSpinId
                }
                acc
            }

        // Build final results maintaining order
        val items = roundIds.mapNotNull { roundId ->
            val data = roundsData[roundId] ?: return@mapNotNull null
            val amounts = spinAmounts[roundId] ?: SpinAmounts()

            RoundDetailsReadModel(
                id = roundId,
                placeAmount = amounts.placeAmount,
                settleAmount = amounts.settleAmount,
                freeSpinId = amounts.freeSpinId,
                currency = data.currency.value,
                gameIdentity = data.gameIdentity,
                gameName = data.gameName,
                providerName = data.providerName,
                isFinished = data.finished,
                createdAt = data.createdAt,
                finishedAt = data.finishedAt
            )
        }

        Page(
            items = items,
            totalPages = totalPages,
            totalItems = totalCount,
            currentPage = pageable.pageReal
        )
    }

    private fun buildRoundBaseQuery(): Query {
        return RoundTable
            .innerJoin(SessionTable, { RoundTable.sessionId }, { SessionTable.id })
            .innerJoin(GameTable, { RoundTable.gameId }, { GameTable.id })
            .select(RoundTable.id, RoundTable.createdAt)
    }

    private fun buildFullRoundQuery(): Query {
        return RoundTable
            .innerJoin(SessionTable, { RoundTable.sessionId }, { SessionTable.id })
            .innerJoin(GameTable, { RoundTable.gameId }, { GameTable.id })
            .innerJoin(ProviderTable, { GameTable.providerId }, { ProviderTable.id })
            .selectAll()
    }

    private fun Query.applyFilters(filter: RoundFilter): Query = apply {
        filter.playerId?.let { playerId ->
            andWhere { SessionTable.playerId eq playerId }
        }
        filter.gameIdentity?.let { gameIdentity ->
            andWhere { GameTable.identity eq gameIdentity }
        }
    }

    private data class RoundRowData(
        val roundId: UUID,
        val finished: Boolean,
        val createdAt: LocalDateTime,
        val finishedAt: LocalDateTime?,
        val currency: Currency,
        val gameIdentity: String,
        val gameName: String,
        val providerName: String
    )

    private data class SpinAmounts(
        var placeAmount: BigInteger = BigInteger.ZERO,
        var settleAmount: BigInteger = BigInteger.ZERO,
        var freeSpinId: String? = null
    )
}
