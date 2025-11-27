package app.service.spin

import core.error.RoundFinishedError
import core.error.RoundNotFoundError
import domain.game.model.Game
import domain.session.model.Session
import domain.session.table.RoundTable
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsertReturning
import org.koin.core.component.KoinComponent
import java.time.LocalDateTime
import java.util.UUID

abstract class ISpinService {
    abstract suspend fun place(session: Session, game: Game, command: ISpinCommand) : Result<Unit>

    abstract suspend fun settle(session: Session, extRoundId: String, command: ISpinCommand): Result<Unit>

    abstract suspend fun rollback(session: Session, command: ISpinCommand): Result<Unit>

    open suspend fun closeRound(session: Session, extRoundId: String): Result<Unit> {
        return newSuspendedTransaction {
            val roundId = findRoundId(session, extRoundId)
                .getOrElse { return@newSuspendedTransaction Result.failure(it) }

            RoundTable.update({ RoundTable.id eq roundId }) {
                it[RoundTable.endAt] = LocalDateTime.now().toKotlinLocalDateTime()
            }

            return@newSuspendedTransaction Result.success(Unit)
        }
    }

    protected suspend fun findRoundId(session: Session, extRoundId: String): Result<UUID> {
        return newSuspendedTransaction {
            val round = RoundTable.selectAll()
                .where { RoundTable.extId eq extRoundId and (RoundTable.sessionId eq session.id) }
                .singleOrNull() ?: return@newSuspendedTransaction Result.failure(RoundNotFoundError())

            if (round[RoundTable.endAt] != null) {
                return@newSuspendedTransaction Result.failure(RoundFinishedError())
            }

            Result.success(round[RoundTable.id].value)
        }
    }

    protected suspend fun createRoundId(session: Session, game: Game, command: ISpinCommand): Result<UUID> {
        return newSuspendedTransaction {
            val round = RoundTable.upsertReturning(
                keys = arrayOf(RoundTable.extId, RoundTable.sessionId),
                onUpdateExclude = listOf(RoundTable.endAt, RoundTable.createdAt),
            ) {
                it[RoundTable.sessionId] = session.id
                it[RoundTable.gameId] = game.id
                it[RoundTable.extId] = command.extRoundId

                if (command is FreespinSpinCommand) {
                    it[RoundTable.freespinId] = command.referenceId
                }

            }.single()

            if (round[RoundTable.endAt] != null) {
                return@newSuspendedTransaction Result.failure(RoundFinishedError())
            }

            Result.success(round[RoundTable.id].value)
        }
    }
}

class SpinServiceSpec : ISpinService(), KoinComponent {
    val spinService = getKoin().get<SpinService>()
    val freeSpinService = getKoin().get<FreeSpinService>()

    override suspend fun place(
        session: Session,
        game: Game,
        command: ISpinCommand
    ): Result<Unit> {
        if (command is FreespinSpinCommand) {
            return freeSpinService.place(session, game, command)
        }
        return spinService.place(session, game, command)
    }

    override suspend fun settle(
        session: Session,
        extRoundId: String,
        command: ISpinCommand
    ): Result<Unit> {
        if (command is FreespinSpinCommand) {
            return freeSpinService.settle(session, extRoundId, command)
        }

        return spinService.settle(session, extRoundId, command)
    }

    override suspend fun rollback(session: Session, command: ISpinCommand): Result<Unit> {
        if (command is FreespinSpinCommand) {
            return freeSpinService.rollback(session, command)
        }
        return spinService.rollback(session, command)
    }
}