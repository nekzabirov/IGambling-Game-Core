package app.service

import app.adapter.PlayerAdapter
import app.adapter.WalletAdapter
import core.error.BetOutLimitError
import core.error.InsufficientBalanceError
import core.error.RoundFinishedError
import core.model.Balance
import core.model.BetAmount
import core.model.SpinType
import core.value.SessionToken
import domain.aggregator.mapper.toAggregatorModel
import domain.aggregator.model.Aggregator
import domain.aggregator.model.AggregatorInfo
import domain.aggregator.table.AggregatorInfoTable
import domain.game.mapper.toGame
import domain.game.model.Game
import domain.game.table.GameTable
import domain.game.table.GameVariantTable
import domain.session.mapper.toSession
import domain.session.model.Session
import domain.session.table.RoundTable
import domain.session.table.SessionTable
import domain.session.table.SpinTable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import org.jetbrains.exposed.sql.upsertReturning
import org.koin.core.component.KoinComponent
import java.time.LocalDateTime
import java.util.UUID

object SpinService : KoinComponent {
    val walletAdapter = getKoin().get<WalletAdapter>()
    val playerAdapter = getKoin().get<PlayerAdapter>()

    suspend fun findBalance(token: SessionToken, gameSymbol: String? = null): Result<Balance> {
        val (session, game) = newSuspendedTransaction {
            val session = SessionTable.selectAll()
                .where { SessionTable.token eq token.value }
                .single().toSession()

            val game = if (gameSymbol != null) {
                val aggregatorType = AggregatorInfoTable.select(AggregatorInfoTable.aggregator)
                    .where { AggregatorInfoTable.id eq session.aggregatorId }
                    .single()[AggregatorInfoTable.aggregator]

                GameVariantTable
                    .innerJoin(GameTable, { GameVariantTable.gameId }, { GameTable.id })
                    .select(GameTable.columns)
                    .where { GameVariantTable.symbol eq gameSymbol and (GameVariantTable.aggregator eq aggregatorType) }
                    .single().toGame()
            } else {
                GameTable.selectAll()
                    .where { GameTable.id eq session.gameId }
                    .single().toGame()
            }

            session to game
        }

        return findBalance(session, game)
    }

    suspend fun place(
        token: SessionToken,
        gameSymbol: String,
        extRoundId: String,
        transactionId: String,
        amount: Int,
        freespinId: String? = null,
    ): Result<Unit> {
        val (session, aggregatorInfo, game) = newSuspendedTransaction {
            val session = findSession(token).getOrElse { return@newSuspendedTransaction null }

            val aggregatorInfo = findAggregator(session.aggregatorId).getOrElse { return@newSuspendedTransaction null }

            val game = findGame(gameSymbol, aggregatorInfo.aggregator).getOrElse { return@newSuspendedTransaction null }

            Triple(session, aggregatorInfo, game)
        } ?: return Result.failure(NoSuchElementException())

        val betAmount = coroutineScope {
            if (freespinId != null) {
                return@coroutineScope Result.success(BetAmount(real = 0, bonus = 0, currency = session.currency))
            }

            val balanceDeferred = async { findBalance(session, game) }
            val betLimitDeferred = async { playerAdapter.findCurrentBetLimit(session.playerId) }

            val balance = balanceDeferred.await().getOrElse { return@coroutineScope Result.failure(it) }
            val playerBetLimit = betLimitDeferred.await().getOrElse { return@coroutineScope Result.failure(it) }

            if (amount > balance.totalAmount) {
                return@coroutineScope Result.failure(InsufficientBalanceError())
            }

            if (playerBetLimit != null && amount > playerBetLimit) {
                return@coroutineScope Result.failure(BetOutLimitError())
            }

            val realAmount = minOf(balance.real, amount)
            val bonusAmount = amount - realAmount

            Result.success(BetAmount(real = realAmount, bonus = bonusAmount, currency = session.currency))
        }
            .getOrElse { return Result.failure(it) }

        if (freespinId == null) {
            walletAdapter.withdraw(
                session.playerId,
                session.id.toString(),
                session.currency,
                betAmount.real,
                betAmount.bonus
            ).getOrElse { return Result.failure(it) }
        }

        return newSuspendedTransaction {
            val round = RoundTable.upsertReturning(
                keys = arrayOf(RoundTable.extId, RoundTable.sessionId),
                onUpdateExclude = listOf(RoundTable.endAt, RoundTable.createdAt),
            ) {
                it[RoundTable.sessionId] = session.id
                it[RoundTable.gameId] = game.id
                it[RoundTable.extId] = extRoundId
                it[RoundTable.freespinId] = freespinId
            }.single()

            if (round[RoundTable.endAt] != null) {
                return@newSuspendedTransaction Result.failure(RoundFinishedError())
            }

            SpinTable.insert {
                it[SpinTable.type] = SpinType.PLACE
                it[SpinTable.amount] = amount
                it[SpinTable.realAmount] = betAmount.real
                it[SpinTable.bonusAmount] = betAmount.bonus
                it[SpinTable.extId] = transactionId
                it[SpinTable.roundId] = round[RoundTable.id].value
            }

            Result.success(Unit)
        }
    }

    suspend fun settle(token: SessionToken, extRoundId: String, transactionId: String, amount: Int): Result<Balance> {
        newSuspendedTransaction {
            val session = findSession(token).getOrElse { return@newSuspendedTransaction null }

            val round = RoundTable.selectAll()
                .where { RoundTable.extId eq extRoundId and (RoundTable.sessionId eq session.id) }
                .singleOrNull() ?: return@newSuspendedTransaction null

            if (round[RoundTable.endAt] != null) {
                return@newSuspendedTransaction null
            }


        }

        val session = findSession(token).getOrElse { return Result.failure(it) }

        val roundId = newSuspendedTransaction {
            val res = RoundTable.select(RoundTable.id, RoundTable.endAt)
                .where { RoundTable.extId eq extRoundId and (RoundTable.sessionId eq session.id) }
                .singleOrNull() ?: return@newSuspendedTransaction null

            if (res[RoundTable.endAt] != null) {
                return@newSuspendedTransaction null
            }

            res[RoundTable.id].value
        }
            ?: return Result.failure(RoundFinishedError())

        val (spinPlaceId, isBonusUsed) = newSuspendedTransaction {
            val spinRow = SpinTable
                .select(SpinTable.id, SpinTable.type, SpinTable.roundId, SpinTable.bonusAmount)
                .where { SpinTable.roundId eq roundId and (SpinTable.type eq SpinType.PLACE) }
                .singleOrNull() ?: return@newSuspendedTransaction null

            val spinPlaceId = spinRow[SpinTable.id].value
            val isBonusUsed = spinRow[SpinTable.bonusAmount] > 0

            spinPlaceId to isBonusUsed
        } ?: return Result.failure(RoundFinishedError())

        val realAmount = if (isBonusUsed) 0 else amount
        val bonusAmount = if (isBonusUsed) amount else 0

        walletAdapter.deposit(session.playerId, session.id.toString(), session.currency, realAmount, bonusAmount)
            .getOrElse { return Result.failure(it) }

        newSuspendedTransaction {
            SpinTable.insert {
                it[SpinTable.type] = SpinType.SETTLE
                it[SpinTable.amount] = amount
                it[SpinTable.realAmount] = realAmount
                it[SpinTable.bonusAmount] = bonusAmount
                it[SpinTable.extId] = transactionId
                it[SpinTable.roundId] = roundId
                it[SpinTable.referenceId] = spinPlaceId
            }
        }

        return findBalance(token)
    }

    suspend fun closeRound(token: SessionToken, extRoundId: String): Result<Unit> {
        return newSuspendedTransaction {
            val session = findSession(token).getOrElse { return@newSuspendedTransaction Result.failure(it) }

            val round = RoundTable.selectAll()
                .where { RoundTable.extId eq extRoundId and (RoundTable.sessionId eq session.id) }
                .singleOrNull() ?: return@newSuspendedTransaction Result.failure(Exception("Round not found"))

            if (round[RoundTable.endAt] != null) {
                return@newSuspendedTransaction Result.failure(Exception("Round is end"))
            }

            RoundTable.update({ RoundTable.id eq round[RoundTable.id].value }) {
                it[RoundTable.endAt] = LocalDateTime.now().toKotlinLocalDateTime()
            }

            return@newSuspendedTransaction Result.success(Unit)
        }
    }

    private suspend fun findSession(token: SessionToken): Result<Session> {
        return newSuspendedTransaction {
            SessionTable.selectAll()
                .where { SessionTable.token eq token.value }
                .singleOrNull()
                ?.toSession()
                ?.let { Result.success(it) }
                ?: Result.failure(NoSuchElementException())
        }
    }

    private suspend fun findGame(gameSymbol: String, aggregator: Aggregator): Result<Game> {
        return newSuspendedTransaction {
            GameVariantTable
                .innerJoin(GameTable, { GameVariantTable.gameId }, { GameTable.id })
                .select(GameTable.columns)
                .where { GameVariantTable.symbol eq gameSymbol and (GameVariantTable.aggregator eq aggregator) }
                .single().toGame()
                .let { Result.success(it) }
        }
    }

    private suspend fun findAggregator(id: UUID): Result<AggregatorInfo> {
        return newSuspendedTransaction {
            AggregatorInfoTable.selectAll()
                .where { AggregatorInfoTable.id eq id }
                .single()
                .let { Result.success(it.toAggregatorModel()) }
        }
    }

    private suspend fun findBalance(session: Session, game: Game): Result<Balance> {
        val balance = walletAdapter.findBalance(session.playerId).getOrElse {
            return Result.failure(it)
        }

        val isBonusBetAllow = game.bonusBetEnable

        return Result.success(balance.copy(bonus = if (isBonusBetAllow) balance.bonus else 0))
    }
}