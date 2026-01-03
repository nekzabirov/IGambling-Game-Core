package com.nekgamebling.infrastructure.handler.spin.query

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.spin.FindRoundQuery
import com.nekgamebling.application.port.inbound.spin.FindRoundQueryResult
import domain.common.error.NotFoundError
import domain.common.value.SpinType
import domain.session.model.Round
import infrastructure.persistence.exposed.table.GameTable
import infrastructure.persistence.exposed.table.ProviderTable
import infrastructure.persistence.exposed.table.RoundTable
import infrastructure.persistence.exposed.table.SessionTable
import infrastructure.persistence.exposed.table.SpinTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class FindRoundQueryHandler : QueryHandler<FindRoundQuery, FindRoundQueryResult> {

    override suspend fun handle(query: FindRoundQuery): Result<FindRoundQueryResult> = newSuspendedTransaction {
        val roundId = try {
            UUID.fromString(query.id)
        } catch (e: IllegalArgumentException) {
            return@newSuspendedTransaction Result.failure(NotFoundError("Round", query.id))
        }

        // Get round with related details
        val row = RoundTable
            .innerJoin(SessionTable, { RoundTable.sessionId }, { SessionTable.id })
            .innerJoin(GameTable, { RoundTable.gameId }, { GameTable.id })
            .innerJoin(ProviderTable, { GameTable.providerId }, { ProviderTable.id })
            .selectAll()
            .where { RoundTable.id eq roundId }
            .firstOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Round", query.id))

        val round = Round(
            id = row[RoundTable.id].value,
            sessionId = row[RoundTable.sessionId].value,
            gameId = row[RoundTable.gameId].value,
            extId = row[RoundTable.extId],
            finished = row[RoundTable.finished],
            createdAt = row[RoundTable.createdAt],
            finishedAt = row[RoundTable.finishedAt]
        )

        // Get spin aggregations
        val placeAmounts = SpinTable
            .select(SpinTable.realAmount.sum(), SpinTable.bonusAmount.sum())
            .where { (SpinTable.roundId eq roundId) and (SpinTable.type eq SpinType.PLACE) }
            .firstOrNull()
            ?.let {
                Pair(it[SpinTable.realAmount.sum()] ?: 0L, it[SpinTable.bonusAmount.sum()] ?: 0L)
            } ?: Pair(0L, 0L)

        val settleAmounts = SpinTable
            .select(SpinTable.realAmount.sum(), SpinTable.bonusAmount.sum())
            .where { (SpinTable.roundId eq roundId) and (SpinTable.type eq SpinType.SETTLE) }
            .firstOrNull()
            ?.let {
                Pair(it[SpinTable.realAmount.sum()] ?: 0L, it[SpinTable.bonusAmount.sum()] ?: 0L)
            } ?: Pair(0L, 0L)

        Result.success(
            FindRoundQueryResult(
                round = round,
                providerIdentity = row[ProviderTable.identity],
                gameIdentity = row[GameTable.identity],
                playerId = row[SessionTable.playerId],
                totalPlaceReal = placeAmounts.first,
                totalPlaceBonus = placeAmounts.second,
                totalSettleReal = settleAmounts.first,
                totalSettleBonus = settleAmounts.second
            )
        )
    }
}
