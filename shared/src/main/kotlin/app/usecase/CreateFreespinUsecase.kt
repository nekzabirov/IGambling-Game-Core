package app.usecase

import core.value.Currency
import domain.aggregator.adapter.command.CreateFreenspinCommand
import service.GameService
import infrastructure.aggregator.AggregatorFabric
import io.ktor.server.plugins.BadRequestException
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CreateFreespinUsecase {
    suspend operator fun invoke(
        presetValue: Map<String, Int>,
        referenceId: String,
        playerId: String,
        gameIdentity: String,
        currency: Currency,
        startAt: LocalDateTime,
        endAt: LocalDateTime,
    ): Result<Unit> = newSuspendedTransaction {
        val game = GameService.findByIdentity(gameIdentity)
            .getOrElse { return@newSuspendedTransaction Result.failure(it) }

        if (!game.freeSpinEnable) {
            return@newSuspendedTransaction Result.failure(BadRequestException("Free spin is not enabled"))
        }

        val adapter = AggregatorFabric.createAdapter(game.aggregator.config, game.aggregator.aggregator)

        adapter.createFreespin(
            CreateFreenspinCommand(
                presetValue = presetValue,
                referenceId = referenceId,
                playerId = playerId,
                gameSymbol = game.symbol,
                currency = currency,
                startAt = startAt,
                endAt = endAt
            )
        ).getOrElse { return@newSuspendedTransaction Result.failure(it) }

        Result.success(Unit)
    }
}
