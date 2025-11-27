package app.service.spin

import core.error.RoundNotFoundError
import core.model.SpinType
import domain.game.model.Game
import domain.session.model.Session
import domain.session.table.RoundTable
import domain.session.table.SpinTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.component.KoinComponent

class FreeSpinService : ISpinService(), KoinComponent {
    override suspend fun place(session: Session, game: Game, command: ISpinCommand): Result<Unit> {
        return newSuspendedTransaction {
            val roundId = createRoundId(session, game, command)
                .getOrElse { return@newSuspendedTransaction Result.failure(it) }

            SpinTable.insert {
                it[SpinTable.type] = SpinType.PLACE
                it[SpinTable.amount] = command.amount
                it[SpinTable.realAmount] = 0
                it[SpinTable.bonusAmount] = 0
                it[SpinTable.extId] = command.extRoundId
                it[SpinTable.roundId] = roundId
            }

            Result.success(Unit)
        }
    }

    override suspend fun settle(session: Session, extRoundId: String, command: ISpinCommand): Result<Unit> {
        return newSuspendedTransaction {
            val roundIdN = findRoundId(session, extRoundId)
                .getOrElse { return@newSuspendedTransaction Result.failure(it) }

            SpinTable.insert {
                it[SpinTable.type] = SpinType.SETTLE
                it[SpinTable.amount] = command.amount
                it[SpinTable.realAmount] = 0
                it[SpinTable.bonusAmount] = 0
                it[SpinTable.extId] = command.transactionId
                it[SpinTable.roundId] = roundIdN
            }

            Result.success(Unit)
        }
    }

    override suspend fun rollback(session: Session, command: ISpinCommand): Result<Unit> {
        return newSuspendedTransaction {
            val spinId = SpinTable.innerJoin(RoundTable, { RoundTable.id }, { SpinTable.roundId })
                .select(SpinTable.id)
                .where { RoundTable.extId eq command.transactionId and (RoundTable.sessionId eq session.id) }
                .singleOrNull()?.get(SpinTable.id) ?: return@newSuspendedTransaction Result.failure(RoundNotFoundError())

            SpinTable.insert {
                it[SpinTable.type] = SpinType.ROLLBACK
                it[SpinTable.extId] = command.transactionId
                it[SpinTable.referenceId] = spinId
            }

            return@newSuspendedTransaction Result.success(Unit)
        }
    }
}