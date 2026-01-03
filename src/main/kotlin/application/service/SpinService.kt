package application.service

import application.port.outbound.PlayerAdapter
import application.port.outbound.WalletAdapter
import domain.common.error.*
import domain.game.model.Game
import domain.session.model.Round
import domain.session.model.Session
import domain.session.model.Spin
import domain.common.value.SpinType
import infrastructure.persistence.exposed.mapper.toRound
import infrastructure.persistence.exposed.mapper.toSpin
import infrastructure.persistence.exposed.table.RoundTable
import infrastructure.persistence.exposed.table.SpinTable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigInteger
import java.util.UUID

/**
 * Command object for placing/settling spins.
 */
data class SpinCommand(
    val extRoundId: String,
    val transactionId: String,
    val amount: BigInteger,
    val freeSpinId: String? = null
) {
    class Builder {
        private var extRoundId: String = ""
        private var transactionId: String = ""
        private var amount: BigInteger = BigInteger.ZERO
        private var freeSpinId: String? = null

        fun extRoundId(value: String) = apply { extRoundId = value }
        fun transactionId(value: String) = apply { transactionId = value }
        fun amount(value: BigInteger) = apply { amount = value }
        fun freeSpinId(value: String?) = apply { freeSpinId = value }

        fun build() = SpinCommand(extRoundId, transactionId, amount, freeSpinId)
    }

    companion object {
        fun builder() = Builder()
    }
}

/**
 * Application service for spin-related operations.
 * Uses constructor injection for all dependencies.
 */
class SpinService(
    private val walletAdapter: WalletAdapter,
    private val playerAdapter: PlayerAdapter
) {
    /**
     * Place a spin (bet).
     * If freeSpinId is provided, skip wallet operations (freespin mode).
     */
    suspend fun place(session: Session, game: Game, command: SpinCommand): Result<Unit> {
        val isFreeSpin = command.freeSpinId != null

        // Create or get round
        val round = findOrCreateRound(session.id, game.id, command.extRoundId)

        if (isFreeSpin) {
            // FreeSpin mode: just save to DB, no wallet operations
            val spin = Spin(
                id = UUID.randomUUID(),
                roundId = round.id,
                type = SpinType.PLACE,
                amount = command.amount,
                realAmount = BigInteger.ZERO,
                bonusAmount = BigInteger.ZERO,
                extId = command.transactionId,
                freeSpinId = command.freeSpinId
            )

            saveSpin(spin)
            return Result.success(Unit)
        }

        // Normal mode: use wallet
        // Fetch balance and bet limit in parallel
        val (balance, betLimit) = coroutineScope {
            val balanceDeferred = async { walletAdapter.findBalance(session.playerId) }
            val betLimitDeferred = async { playerAdapter.findCurrentBetLimit(session.playerId) }

            val balanceResult = balanceDeferred.await().getOrElse {
                return@coroutineScope Result.failure(it)
            }
            val betLimitResult = betLimitDeferred.await().getOrElse {
                return@coroutineScope Result.failure(it)
            }

            // Adjust balance if bonus bet is disabled
            val adjustedBalance = if (!game.bonusBetEnable) {
                balanceResult.copy(bonus = BigInteger.ZERO)
            } else {
                balanceResult
            }

            Result.success(adjustedBalance to betLimitResult)
        }.getOrElse { return Result.failure(it) }

        // Validate bet limit
        if (betLimit != null && betLimit < command.amount) {
            return Result.failure(
                BetLimitExceededError(session.playerId, command.amount, betLimit)
            )
        }

        // Validate sufficient balance
        if (command.amount > balance.totalAmount) {
            return Result.failure(
                InsufficientBalanceError(session.playerId, command.amount, balance.totalAmount)
            )
        }

        // Calculate real and bonus amounts
        val betRealAmount = minOf(command.amount, balance.real)
        val betBonusAmount = command.amount - betRealAmount

        // Create spin record
        val spin = Spin(
            id = UUID.randomUUID(),
            roundId = round.id,
            type = SpinType.PLACE,
            amount = command.amount,
            realAmount = betRealAmount,
            bonusAmount = betBonusAmount,
            extId = command.transactionId
        ).let { saveSpin(it) }

        // Withdraw from wallet
        walletAdapter.withdraw(
            playerId = session.playerId,
            transactionId = spin.id.toString(),
            currency = session.currency,
            realAmount = betRealAmount,
            bonusAmount = betBonusAmount
        ).getOrElse {
            return Result.failure(it)
        }

        return Result.success(Unit)
    }

    /**
     * Settle a spin (determine win/loss).
     * If freeSpinId is provided, skip wallet operations (freespin mode).
     */
    suspend fun settle(session: Session, extRoundId: String, command: SpinCommand): Result<Unit> {
        val isFreeSpin = command.freeSpinId != null

        // Find the round
        val round = findRoundByExtId(session.id, extRoundId)
            ?: return Result.failure(RoundNotFoundError(extRoundId))

        // Find the place spin
        val placeSpin = findPlaceSpinByRoundId(round.id)
            ?: return Result.failure(RoundFinishedError(extRoundId))

        if (isFreeSpin) {
            // FreeSpin mode: just save to DB, no wallet operations
            val settleSpin = Spin(
                id = UUID.randomUUID(),
                roundId = round.id,
                type = SpinType.SETTLE,
                amount = command.amount,
                realAmount = BigInteger.ZERO,
                bonusAmount = BigInteger.ZERO,
                extId = command.transactionId,
                referenceId = placeSpin.id,
                freeSpinId = command.freeSpinId
            )

            saveSpin(settleSpin)
            return Result.success(Unit)
        }

        // Normal mode: use wallet
        // Determine if bonus was used
        val isBonusUsed = placeSpin.bonusAmount > BigInteger.ZERO

        // Calculate win amounts
        val realAmount = if (isBonusUsed) BigInteger.ZERO else command.amount
        val bonusAmount = if (isBonusUsed) command.amount else BigInteger.ZERO

        // Create settle spin
        val settleSpin = Spin(
            id = UUID.randomUUID(),
            roundId = round.id,
            type = SpinType.SETTLE,
            amount = command.amount,
            realAmount = realAmount,
            bonusAmount = bonusAmount,
            extId = command.transactionId,
            referenceId = placeSpin.id
        ).let { saveSpin(it) }

        // Deposit winnings
        walletAdapter.deposit(
            playerId = session.playerId,
            transactionId = settleSpin.id.toString(),
            currency = session.currency,
            realAmount = realAmount,
            bonusAmount = bonusAmount
        ).getOrElse {
            return Result.failure(it)
        }

        return Result.success(Unit)
    }

    /**
     * Rollback a spin.
     * If freeSpinId is provided, skip wallet operations (freespin mode).
     */
    suspend fun rollback(session: Session, command: SpinCommand): Result<Unit> {
        // Find the round
        val round = findRoundByExtId(session.id, command.extRoundId)
            ?: return Result.failure(RoundNotFoundError(command.extRoundId))

        // Find the spin to rollback
        val spin = findSpinsByRoundId(round.id).firstOrNull()
            ?: return Result.failure(RoundNotFoundError(command.extRoundId))

        val isFreeSpin = spin.freeSpinId != null

        // Create rollback spin
        val rollbackSpin = Spin(
            id = UUID.randomUUID(),
            roundId = round.id,
            type = SpinType.ROLLBACK,
            amount = BigInteger.ZERO,
            realAmount = BigInteger.ZERO,
            bonusAmount = BigInteger.ZERO,
            extId = command.transactionId,
            referenceId = spin.id,
            freeSpinId = command.freeSpinId
        )

        saveSpin(rollbackSpin)

        if (!isFreeSpin) {
            // Normal mode: rollback in wallet
            walletAdapter.rollback(session.playerId, spin.id.toString())
        }

        return Result.success(Unit)
    }

    /**
     * Close a round.
     */
    suspend fun closeRound(session: Session, extRoundId: String): Result<Unit> {
        val round = findRoundByExtId(session.id, extRoundId)
            ?: return Result.failure(RoundNotFoundError(extRoundId))

        finishRound(round.id)

        return Result.success(Unit)
    }

    // Private helper methods using direct Exposed DSL

    private suspend fun findOrCreateRound(sessionId: UUID, gameId: UUID, extId: String): Round =
        newSuspendedTransaction {
            val existing = RoundTable.selectAll()
                .where { (RoundTable.sessionId eq sessionId) and (RoundTable.extId eq extId) }
                .singleOrNull()
                ?.toRound()

            existing ?: run {
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
        }

    private suspend fun findRoundByExtId(sessionId: UUID, extId: String): Round? =
        newSuspendedTransaction {
            RoundTable.selectAll()
                .where { (RoundTable.sessionId eq sessionId) and (RoundTable.extId eq extId) }
                .singleOrNull()
                ?.toRound()
        }

    private suspend fun findPlaceSpinByRoundId(roundId: UUID): Spin? =
        newSuspendedTransaction {
            SpinTable.selectAll()
                .where { (SpinTable.roundId eq roundId) and (SpinTable.type eq SpinType.PLACE) }
                .singleOrNull()
                ?.toSpin()
        }

    private suspend fun findSpinsByRoundId(roundId: UUID): List<Spin> =
        newSuspendedTransaction {
            SpinTable.selectAll()
                .where { SpinTable.roundId eq roundId }
                .map { it.toSpin() }
        }

    private suspend fun saveSpin(spin: Spin): Spin =
        newSuspendedTransaction {
            val id = SpinTable.insertAndGetId {
                it[roundId] = spin.roundId
                it[type] = spin.type
                it[amount] = spin.amount.toLong()
                it[realAmount] = spin.realAmount.toLong()
                it[bonusAmount] = spin.bonusAmount.toLong()
                it[extId] = spin.extId
                it[referenceId] = spin.referenceId
                it[freeSpinId] = spin.freeSpinId
            }
            spin.copy(id = id.value)
        }

    private suspend fun finishRound(roundId: UUID): Unit =
        newSuspendedTransaction {
            RoundTable.update({ RoundTable.id eq roundId }) {
                it[finished] = true
                it[finishedAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            }
        }
}
