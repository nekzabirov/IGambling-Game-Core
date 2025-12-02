package infrastructure.aggregator.pragmatic.adapter

import application.port.outbound.AggregatorFreespinPort
import domain.aggregator.model.AggregatorInfo
import infrastructure.aggregator.pragmatic.model.PragmaticConfig
import infrastructure.aggregator.pragmatic.client.PragmaticHttpClient
import shared.value.Currency
import kotlinx.datetime.LocalDateTime

/**
 * Pragmatic implementation for freespin operations.
 */
class PragmaticFreespinAdapter(
    private val aggregatorInfo: AggregatorInfo,
    private val providerCurrencyAdapter: PragmaticCurrencyAdapter
) : AggregatorFreespinPort {

    private val config = PragmaticConfig(aggregatorInfo.config)
    private val client = PragmaticHttpClient(config)

    override suspend fun getPreset(gameSymbol: String): Result<Map<String, Any>> {
        return Result.success(
            mapOf(
                "totalBet" to mapOf(
                    "minimal" to 100,
                ),
                "rounds" to mapOf(
                    "minimal" to 10
                )
            )
        )
    }

    override suspend fun createFreespin(
        presetValue: Map<String, Int>,
        referenceId: String,
        playerId: String,
        gameSymbol: String,
        currency: Currency,
        startAt: LocalDateTime,
        endAt: LocalDateTime
    ): Result<Unit> {
        TODO("Not implemented: Create freespin in Pragmatic")
    }

    override suspend fun cancelFreespin(
        referenceId: String,
    ): Result<Unit> {
        TODO("Not implemented: Cancel freespin in Pragmatic")
    }
}
