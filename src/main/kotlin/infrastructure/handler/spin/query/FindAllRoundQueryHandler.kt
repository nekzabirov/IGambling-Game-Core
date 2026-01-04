package com.nekgamebling.infrastructure.handler.spin.query

import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.spin.FindAllRoundQuery
import com.nekgamebling.application.port.inbound.spin.FindAllRoundQueryResult
import com.nekgamebling.application.port.inbound.spin.RoundItem
import domain.common.value.SpinType
import domain.game.model.Game
import domain.provider.model.Provider
import domain.session.model.Round
import infrastructure.persistence.exposed.table.GameTable
import infrastructure.persistence.exposed.table.ProviderTable
import infrastructure.persistence.exposed.table.RoundTable
import infrastructure.persistence.exposed.table.SessionTable
import infrastructure.persistence.exposed.table.SpinTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Currency
import shared.value.Page
import java.util.UUID

class FindAllRoundQueryHandler : QueryHandler<FindAllRoundQuery, FindAllRoundQueryResult> {

    override suspend fun handle(query: FindAllRoundQuery): Result<FindAllRoundQueryResult> = newSuspendedTransaction {
        // Aggregation expressions for spin amounts
        val totalPlaceReal = SpinTable.realAmount.sum()
        val totalPlaceBonus = SpinTable.bonusAmount.sum()
        val totalSettleReal = SpinTable.realAmount.sum()
        val totalSettleBonus = SpinTable.bonusAmount.sum()

        // Build base query with JOINs
        val baseQuery = RoundTable
            .innerJoin(SessionTable, { RoundTable.sessionId }, { SessionTable.id })
            .innerJoin(GameTable, { RoundTable.gameId }, { GameTable.id })
            .innerJoin(ProviderTable, { GameTable.providerId }, { ProviderTable.id })
            .leftJoin(SpinTable, { RoundTable.id }, { SpinTable.roundId })

        // Apply filters
        val conditions = mutableListOf<Op<Boolean>>()

        query.gameIdentity?.let { gameIdentity ->
            conditions.add(GameTable.identity eq gameIdentity)
        }

        query.providerIdentity?.let { providerIdentity ->
            conditions.add(ProviderTable.identity eq providerIdentity)
        }

        query.finished?.let { finished ->
            conditions.add(RoundTable.finished eq finished)
        }

        query.playerId?.let { playerId ->
            conditions.add(SessionTable.playerId eq playerId)
        }

        query.freeSpinId?.let { freeSpinId ->
            conditions.add(SpinTable.freeSpinId eq freeSpinId)
        }

        val whereClause = if (conditions.isNotEmpty()) {
            conditions.reduce { acc, op -> acc and op }
        } else {
            Op.TRUE
        }

        // Count total items for pagination
        val totalItems = RoundTable
            .innerJoin(SessionTable, { RoundTable.sessionId }, { SessionTable.id })
            .innerJoin(GameTable, { RoundTable.gameId }, { GameTable.id })
            .innerJoin(ProviderTable, { GameTable.providerId }, { ProviderTable.id })
            .let { baseJoin ->
                if (query.freeSpinId != null) {
                    baseJoin.leftJoin(SpinTable, { RoundTable.id }, { SpinTable.roundId })
                } else {
                    baseJoin
                }
            }
            .select(RoundTable.id.countDistinct())
            .where { whereClause }
            .first()[RoundTable.id.countDistinct()]

        val totalPages = query.pageable.getTotalPages(totalItems)

        // Get round IDs with pagination first
        val roundIds = RoundTable
            .innerJoin(SessionTable, { RoundTable.sessionId }, { SessionTable.id })
            .innerJoin(GameTable, { RoundTable.gameId }, { GameTable.id })
            .innerJoin(ProviderTable, { GameTable.providerId }, { ProviderTable.id })
            .let { baseJoin ->
                if (query.freeSpinId != null) {
                    baseJoin.leftJoin(SpinTable, { RoundTable.id }, { SpinTable.roundId })
                } else {
                    baseJoin
                }
            }
            .select(RoundTable.id)
            .where { whereClause }
            .groupBy(RoundTable.id)
            .orderBy(RoundTable.createdAt to SortOrder.DESC)
            .limit(query.pageable.sizeReal)
            .offset(query.pageable.offset.toLong())
            .map { it[RoundTable.id].value }

        if (roundIds.isEmpty()) {
            return@newSuspendedTransaction Result.success(
                FindAllRoundQueryResult(
                    items = Page.empty(),
                    providers = emptyList(),
                    games = emptyList()
                )
            )
        }

        // Data class to hold round with related identities
        data class RoundWithDetails(
            val round: Round,
            val gameId: UUID,
            val providerId: UUID,
            val gameIdentity: String,
            val providerIdentity: String,
            val playerId: String,
            val currency: Currency
        )

        // Get rounds with all details
        val roundsWithDetails = RoundTable
            .innerJoin(SessionTable, { RoundTable.sessionId }, { SessionTable.id })
            .innerJoin(GameTable, { RoundTable.gameId }, { GameTable.id })
            .innerJoin(ProviderTable, { GameTable.providerId }, { ProviderTable.id })
            .selectAll()
            .where { RoundTable.id inList roundIds }
            .orderBy(RoundTable.createdAt to SortOrder.DESC)
            .map { row ->
                RoundWithDetails(
                    round = Round(
                        id = row[RoundTable.id].value,
                        sessionId = row[RoundTable.sessionId].value,
                        gameId = row[RoundTable.gameId].value,
                        extId = row[RoundTable.extId],
                        finished = row[RoundTable.finished],
                        createdAt = row[RoundTable.createdAt],
                        finishedAt = row[RoundTable.finishedAt]
                    ),
                    gameId = row[GameTable.id].value,
                    providerId = row[ProviderTable.id].value,
                    gameIdentity = row[GameTable.identity],
                    providerIdentity = row[ProviderTable.identity],
                    playerId = row[SessionTable.playerId],
                    currency = Currency(row[SessionTable.currency])
                )
            }

        // Get spin aggregations per round
        val placeAmounts = SpinTable
            .select(
                SpinTable.roundId,
                SpinTable.realAmount.sum(),
                SpinTable.bonusAmount.sum()
            )
            .where { (SpinTable.roundId inList roundIds) and (SpinTable.type eq SpinType.PLACE) }
            .groupBy(SpinTable.roundId)
            .associate { row ->
                row[SpinTable.roundId]!!.value to Pair(
                    row[SpinTable.realAmount.sum()] ?: 0L,
                    row[SpinTable.bonusAmount.sum()] ?: 0L
                )
            }

        val settleAmounts = SpinTable
            .select(
                SpinTable.roundId,
                SpinTable.realAmount.sum(),
                SpinTable.bonusAmount.sum()
            )
            .where { (SpinTable.roundId inList roundIds) and (SpinTable.type eq SpinType.SETTLE) }
            .groupBy(SpinTable.roundId)
            .associate { row ->
                row[SpinTable.roundId]!!.value to Pair(
                    row[SpinTable.realAmount.sum()] ?: 0L,
                    row[SpinTable.bonusAmount.sum()] ?: 0L
                )
            }

        // Build round items
        val items = roundsWithDetails.map { details ->
            val placeAmt = placeAmounts[details.round.id] ?: Pair(0L, 0L)
            val settleAmt = settleAmounts[details.round.id] ?: Pair(0L, 0L)
            RoundItem(
                round = details.round,
                providerIdentity = details.providerIdentity,
                gameIdentity = details.gameIdentity,
                playerId = details.playerId,
                currency = details.currency,
                totalPlaceReal = placeAmt.first,
                totalPlaceBonus = placeAmt.second,
                totalSettleReal = settleAmt.first,
                totalSettleBonus = settleAmt.second
            )
        }

        // Get distinct game IDs and provider IDs
        val gameIds = roundsWithDetails.map { it.gameId }.distinct()
        val providerIds = roundsWithDetails.map { it.providerId }.distinct()

        // Load games
        val games = GameTable
            .selectAll()
            .where { GameTable.id inList gameIds }
            .map { row ->
                Game(
                    id = row[GameTable.id].value,
                    identity = row[GameTable.identity],
                    name = row[GameTable.name],
                    providerId = row[GameTable.providerId].value,
                    images = row[GameTable.images],
                    bonusBetEnable = row[GameTable.bonusBetEnable],
                    bonusWageringEnable = row[GameTable.bonusWageringEnable],
                    tags = row[GameTable.tags],
                    active = row[GameTable.active]
                )
            }

        // Load providers
        val providers = ProviderTable
            .selectAll()
            .where { ProviderTable.id inList providerIds }
            .map { row ->
                Provider(
                    id = row[ProviderTable.id].value,
                    identity = row[ProviderTable.identity],
                    name = row[ProviderTable.name],
                    images = row[ProviderTable.images],
                    order = row[ProviderTable.order],
                    aggregatorId = row[ProviderTable.aggregatorId]?.value,
                    active = row[ProviderTable.active]
                )
            }

        Result.success(
            FindAllRoundQueryResult(
                items = Page(
                    items = items,
                    totalPages = totalPages,
                    totalItems = totalItems,
                    currentPage = query.pageable.pageReal
                ),
                providers = providers,
                games = games
            )
        )
    }
}
