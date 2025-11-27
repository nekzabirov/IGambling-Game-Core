package app.usecase

import domain.aggregator.adapter.command.CancelFreespinCommand
import app.service.GameService
import infrastructure.aggregator.AggregatorFabric
import io.ktor.server.plugins.BadRequestException
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CancelFreespinUsecase {
    suspend operator fun invoke(
        referenceId: String,
        gameIdentity: String,
    ): Result<Unit> = newSuspendedTransaction {
        val game = GameService.findByIdentity(gameIdentity)
            .getOrElse { return@newSuspendedTransaction Result.failure(it) }

        if (!game.freeSpinEnable) {
            return@newSuspendedTransaction Result.failure(BadRequestException("Free spin is not enabled"))
        }

        val adapter = AggregatorFabric.createAdapter(game.aggregator.config, game.aggregator.aggregator)

        adapter.cancelFreespin(
            CancelFreespinCommand(
                referenceId = referenceId
            )
        ).getOrElse { return@newSuspendedTransaction Result.failure(it) }

        Result.success(Unit)
    }
}
