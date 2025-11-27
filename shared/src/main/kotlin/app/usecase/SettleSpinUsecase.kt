package app.usecase

import app.adapter.EventProducerAdapter
import app.event.SpinEvent
import app.service.spin.ISpinCommand
import app.service.spin.SpinServiceSpec
import core.error.GameUnavailableError
import core.error.SessionUnavailingError
import core.model.SpinType
import core.value.SessionToken
import domain.game.dao.full
import domain.game.mapper.toGameFull
import domain.game.model.Game
import domain.game.table.GameTable
import domain.session.dao.findByToken
import domain.session.table.SessionTable
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.component.KoinComponent

class SettleSpinUsecase : KoinComponent {
    private val spinService = getKoin().get<SpinServiceSpec>()
    private val eventProducer = getKoin().get<EventProducerAdapter>()

    suspend operator fun invoke(
        token: SessionToken,
        extRoundId: String,
        exTransactionId: String,
        freeSpinId: String?,
        amount: Int
    ): Result<Unit> {
        val (session, game) = newSuspendedTransaction {
            val session = SessionTable.findByToken(token.value) ?: return@newSuspendedTransaction Result.failure(
                SessionUnavailingError()
            )

            val game = GameTable.full()
                .andWhere { GameTable.id eq session.gameId }
                .singleOrNull()?.toGameFull() ?: return@newSuspendedTransaction Result.failure(GameUnavailableError())

            Result.success(session to game)
        }.getOrElse { return Result.failure(it) }

        val command = ISpinCommand.Builder()
            .withExtRoundId(extRoundId)
            .withTransactionId(exTransactionId)
            .withAmount(amount)
            .let {
                if (freeSpinId != null) {
                    it.withFreeSpinId(freeSpinId)
                }

                it
            }
            .build()

        spinService.settle(session, extRoundId, command).getOrElse { return Result.failure(it) }

        eventProducer.publish(SpinEvent(
            type = SpinType.SETTLE,
            game = game,
            amount = command.amount,
            currency = session.currency,
            playerId = session.playerId,
            freeSpinId = freeSpinId
        ))

        return Result.success(Unit)
    }
}