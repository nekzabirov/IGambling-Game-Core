package app.usecase

import domain.aggregator.adapter.PresetParam
import service.GameService
import infrastructure.aggregator.AggregatorFabric
import io.ktor.server.plugins.BadRequestException
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class GetPresetUsecase {
    suspend operator fun invoke(gameIdentity: String): Result<Response> = newSuspendedTransaction {
        val game = GameService.findByIdentity(gameIdentity)
            .getOrElse { return@newSuspendedTransaction Result.failure(it) }

        if (!game.freeSpinEnable) {
            return@newSuspendedTransaction Result.failure(BadRequestException("Free spin is not enabled"))
        }

        val adapter = AggregatorFabric.createAdapter(game.aggregator.config, game.aggregator.aggregator)

        val preset = adapter.getPreset(game.symbol)
            .getOrElse { return@newSuspendedTransaction Result.failure(it) }

        val presetMap = preset.toMap().mapValues { (_, param) ->
            mapOf(
                "value" to param.value,
                "default" to param.default,
                "minimal" to param.minimal,
                "maximum" to param.maximum
            )
        }

        Result.success(Response(presetMap))
    }

    data class Response(val preset: Map<String, Map<String, Int?>>)
}
