package app.usecase

import app.service.spin.FreeSpinService
import app.service.spin.SpinService
import core.error.SessionUnavailingError
import core.value.SessionToken
import domain.session.dao.findByToken
import domain.session.table.SessionTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.component.KoinComponent

class CloseRoundUsecase : KoinComponent {
    val spinService = getKoin().get<SpinService>()
    val freeSpinService = getKoin().get<FreeSpinService>()

    suspend operator fun invoke(
        token: SessionToken,
        extRoundId: String,
        freeSpinId: String?,
    ): Result<Unit> {
        val session = newSuspendedTransaction {
            val session = SessionTable.findByToken(token.value) ?: return@newSuspendedTransaction Result.failure(
                SessionUnavailingError()
            )

            Result.success(session)
        }.getOrElse { return Result.failure(it) }

        val service = if (freeSpinId != null) {
            freeSpinService
        } else {
            spinService
        }

        service.closeRound(session, extRoundId)

        return Result.success(Unit)
    }
}