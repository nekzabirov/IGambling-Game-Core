package infrastructure.persistence.exposed.repository

import domain.common.value.SpinType
import domain.session.model.Round
import domain.session.model.RoundDetails
import domain.session.repository.RoundFilter
import domain.session.repository.RoundRepository
import infrastructure.persistence.exposed.mapper.toGameWithDetails
import infrastructure.persistence.exposed.mapper.toRound
import infrastructure.persistence.exposed.table.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Currency
import shared.value.Page
import shared.value.Pageable
import java.math.BigInteger
import java.util.UUID

/**
 * Exposed implementation of RoundRepository.
 */
class ExposedRoundRepository : BaseExposedRepository<Round, RoundTable>(RoundTable), RoundRepository {

    override fun ResultRow.toEntity(): Round = toRound()

    override suspend fun findByExtId(sessionId: UUID, extId: String): Round? = newSuspendedTransaction {
        table.selectAll()
            .where { (RoundTable.sessionId eq sessionId) and (RoundTable.extId eq extId) }
            .singleOrNull()
            ?.toEntity()
    }

    override suspend fun findBySessionId(sessionId: UUID): List<Round> = findAllByRef(RoundTable.sessionId, sessionId)

    override suspend fun save(round: Round): Round = newSuspendedTransaction {
        val id = RoundTable.insertAndGetId {
            it[sessionId] = round.sessionId
            it[gameId] = round.gameId
            it[extId] = round.extId
            it[finished] = round.finished
        }
        round.copy(id = id.value)
    }

    override suspend fun update(round: Round): Round = newSuspendedTransaction {
        RoundTable.update({ RoundTable.id eq round.id }) {
            it[finished] = round.finished
        }
        round
    }

    override suspend fun finish(id: UUID): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        RoundTable.update({ RoundTable.id eq id }) {
            it[finished] = true
            it[finishedAt] = now
        } > 0
    }

    /**
     * Find or create a round using atomic upsert.
     * Uses unique constraint on (sessionId, extId) for conflict resolution.
     *
     * Optimized: Single query instead of SELECT + INSERT (2 queries).
     * Also eliminates race condition where two concurrent requests
     * could both see no existing round and both try to insert.
     */
    override suspend fun findOrCreate(sessionId: UUID, gameId: UUID, extId: String): Round = newSuspendedTransaction {
        val row = RoundTable.upsertReturning(
            keys = arrayOf(RoundTable.sessionId, RoundTable.extId),
            onUpdateExclude = listOf(RoundTable.id, RoundTable.gameId, RoundTable.finished)
        ) {
            it[RoundTable.sessionId] = sessionId
            it[RoundTable.gameId] = gameId
            it[RoundTable.extId] = extId
            it[finished] = false
        }.single()

        row.toRound()
    }

    override suspend fun findAllWithDetails(pageable: Pageable, filter: RoundFilter): Page<RoundDetails> = newSuspendedTransaction {
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
                    game = row.toGameWithDetails()
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
                val freeSpinId = row[SpinTable.freeSpinId]

                val current = acc.getOrPut(roundId) { SpinAmounts() }
                when (type) {
                    SpinType.PLACE -> current.placeAmount += amount.toBigInteger()
                    SpinType.SETTLE -> current.settleAmount += amount.toBigInteger()
                    SpinType.ROLLBACK -> { /* Not included in totals */ }
                }
                if (freeSpinId != null && current.freeSpinId == null) {
                    current.freeSpinId = freeSpinId
                }
                acc
            }

        // Build final results maintaining order
        val items = roundIds.mapNotNull { roundId ->
            val data = roundsData[roundId] ?: return@mapNotNull null
            val amounts = spinAmounts[roundId] ?: SpinAmounts()

            RoundDetails(
                id = roundId,
                placeAmount = amounts.placeAmount,
                settleAmount = amounts.settleAmount,
                freeSpinId = amounts.freeSpinId,
                currency = data.currency,
                game = data.game,
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
            .innerJoin(AggregatorInfoTable, { ProviderTable.aggregatorId }, { AggregatorInfoTable.id })
            .innerJoin(GameVariantTable, { GameVariantTable.gameId }, { GameTable.id }) {
                GameVariantTable.aggregator eq AggregatorInfoTable.aggregator
            }
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
        val game: domain.game.model.GameWithDetails
    )

    private data class SpinAmounts(
        var placeAmount: BigInteger = BigInteger.ZERO,
        var settleAmount: BigInteger = BigInteger.ZERO,
        var freeSpinId: String? = null
    )
}
