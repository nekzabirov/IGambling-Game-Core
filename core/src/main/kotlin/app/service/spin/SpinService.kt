package app.service.spin

import core.error.BetOutLimitError
import core.error.InsufficientBalanceError
import core.error.RoundFinishedError
import core.error.RoundNotFoundError
import core.model.BetAmount
import core.model.SpinType
import domain.game.model.Game
import domain.session.model.Session
import domain.session.table.RoundTable
import domain.session.table.SessionTable
import domain.session.table.SpinTable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.koin.core.component.KoinComponent
import java.time.LocalDateTime

class SpinService : ISpinService(), KoinComponent {
    val walletAdapter = getKoin().get<app.adapter.WalletAdapter>()
    val playerAdapter = getKoin().get<app.adapter.PlayerAdapter>()

    override suspend fun place(session: Session, game: Game, command: ISpinCommand): Result<Unit> {
        val (balance, betLimit) = coroutineScope {
            val balance = async {
                walletAdapter.findBalance(session.playerId)
            }
            val betLimit = async {
                playerAdapter.findCurrentBetLimit(session.playerId)
            }

            val balanceResult = balance.await()
                .map {
                    if (!game.bonusBetEnable)
                        it.copy(bonus = 0)
                    else it
                }
                .getOrElse { return@coroutineScope Result.failure(it) }
            val betLimitResult = betLimit.await().getOrElse { return@coroutineScope Result.failure(it) }

            return@coroutineScope Result.success(balanceResult to betLimitResult)
        }.getOrElse { return Result.failure(it) }

        if (betLimit != null && betLimit < command.amount) {
            return Result.failure(BetOutLimitError())
        }

        if (command.amount > balance.totalAmount) {
            return Result.failure(InsufficientBalanceError())
        }

        val betRealAmount = if (command.amount > balance.real) balance.real else command.amount
        val betBonusAmount = if (betRealAmount < command.amount) command.amount - betRealAmount else 0

        return newSuspendedTransaction {
            val roundIdN = createRoundId(session, game, command)
                .getOrElse { return@newSuspendedTransaction Result.failure(it) }

            val id = SpinTable.insertAndGetId {
                it[SpinTable.type] = SpinType.PLACE
                it[SpinTable.amount] = command.amount
                it[SpinTable.realAmount] = betRealAmount
                it[SpinTable.bonusAmount] = betBonusAmount
                it[SpinTable.extId] = command.transactionId
                it[SpinTable.roundId] = roundIdN
            }.value

            walletAdapter.withdraw(
                session.playerId,
                id.toString(),
                session.currency,
                betRealAmount,
                betBonusAmount
            ).getOrElse {
                rollback()
                return@newSuspendedTransaction Result.failure(it)
            }

            return@newSuspendedTransaction Result.success(Unit)
        }
    }

    override suspend fun settle(session: Session, extRoundId: String, command: ISpinCommand): Result<Unit> {
        return newSuspendedTransaction {
            val roundIdN = findRoundId(session, extRoundId)
                .getOrElse { return@newSuspendedTransaction Result.failure(it) }

            val spinRow = SpinTable
                .select(SpinTable.id, SpinTable.type, SpinTable.roundId, SpinTable.bonusAmount)
                .where { SpinTable.roundId eq roundIdN and (SpinTable.type eq SpinType.PLACE) }
                .singleOrNull() ?: return@newSuspendedTransaction Result.failure(RoundFinishedError())

            val spinPlaceId = spinRow[SpinTable.id].value
            val isBonusUsed = (spinRow[SpinTable.bonusAmount]?:0) > 0

            val realAmount = if (isBonusUsed) 0 else command.amount
            val bonusAmount = if (isBonusUsed) command.amount else 0

            val betAmount = BetAmount(real = realAmount, bonus = bonusAmount, session.currency)

            val id = SpinTable.insertAndGetId {
                it[SpinTable.type] = SpinType.SETTLE
                it[SpinTable.amount] = command.amount
                it[SpinTable.realAmount] = betAmount.real
                it[SpinTable.bonusAmount] = betAmount.bonus
                it[SpinTable.extId] = command.transactionId
                it[SpinTable.roundId] = roundIdN
                it[SpinTable.referenceId] = spinPlaceId
            }.value

            walletAdapter.deposit(
                session.playerId,
                session.id.toString(),
                session.currency,
                betAmount.real,
                betAmount.bonus
            ).getOrElse {
                rollback()
                return@newSuspendedTransaction Result.failure(it)
            }

            Result.success(Unit)
        }
    }

    override suspend fun rollback(session: Session, command: ISpinCommand): Result<Unit> {
        val spinId = newSuspendedTransaction {
            SpinTable.innerJoin(RoundTable, { RoundTable.id }, { SpinTable.roundId })
                .select(SpinTable.id)
                .where { RoundTable.extId eq command.transactionId and (RoundTable.sessionId eq session.id) }
                .singleOrNull()?.get(SpinTable.id) ?: return@newSuspendedTransaction null
        } ?: throw RoundNotFoundError()

        walletAdapter.rollback(session.playerId, spinId.toString())

        newSuspendedTransaction {
            SpinTable.insert {
                it[SpinTable.type] = SpinType.ROLLBACK
                it[SpinTable.extId] = command.transactionId
                it[SpinTable.referenceId] = spinId
            }
        }

        return Result.success(Unit)
    }
}