package com.nekzabirov.aggregators.adapter.onegamehub

import com.nekzabirov.aggregators.adapter.onegamehub.dto.CancelFreespinDto
import com.nekzabirov.aggregators.adapter.onegamehub.dto.CreateFreespinDto
import com.nekzabirov.aggregators.core.IAggregatorAdapter
import com.nekzabirov.aggregators.core.IAggregatorPreset
import com.nekzabirov.aggregators.adapter.onegamehub.dto.GameDto
import com.nekzabirov.aggregators.adapter.onegamehub.dto.GameUrlDto
import com.nekzabirov.aggregators.adapter.onegamehub.dto.ResponseDto
import com.nekzabirov.aggregators.adapter.onegamehub.model.OneGameHubConfig
import com.nekzabirov.aggregators.adapter.onegamehub.model.OneGameHubPreset
import com.nekzabirov.aggregators.command.CancelFreespinCommand
import com.nekzabirov.aggregators.command.CreateFreenspinCommand
import com.nekzabirov.aggregators.command.CreateLaunchUrlCommand
import com.nekzabirov.aggregators.error.InvalidatePresetError
import com.nekzabirov.aggregators.model.Game
import com.nekzabirov.aggregators.value.Aggregator
import com.nekzabirov.aggregators.value.Platform
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.*

class OneGameHubAdapter(override val config: OneGameHubConfig) : IAggregatorAdapter {
    override val aggregator: Aggregator = Aggregator.ONEGAMEHUB

    private val addressUrl = "https://${config.gateway}/integrations/${config.partner}/rpc"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
                coerceInputValues = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000  // 30 seconds
            connectTimeoutMillis = 10000  // 10 seconds
            socketTimeoutMillis = 30000   // 30 seconds
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }

    override suspend fun listGames(): Result<List<Game>> {
        val response = client.get(addressUrl) {
            setAction("available_games")
        }

        if (!response.status.isSuccess()) {
            return Result.failure(Exception("Failed to fetch games from OneGameHub: ${response.status}"))
        }

        val responseBody = response.body<ResponseDto<List<GameDto>>>()

        if (!responseBody.success) {
            return Result.failure(Exception("Request isn't success. status: ${responseBody.status}"))
        }

        val result = responseBody
            .response?.map {
                Game(
                    symbol = it.id,
                    name = it.name,
                    providerName = it.brand,
                    aggregator = aggregator,
                    freeSpinEnable = it.freespinEnable,
                    freeChipEnable = false,
                    jackpotEnable = false,
                    demoEnable = it.demoEnable,
                    bonusBuyEnable = true,
                    locales = emptyList(),
                    platforms = emptyList()
                )
            }
            ?: emptyList()

        return Result.success(result)
    }

    override suspend fun getPreset(gameSymbol: String): Result<IAggregatorPreset> {
        return OneGameHubPreset(
            quantity = 1,
            betAmount = 10,
            minBetAmount = 10,
            minQuantity = 5,
            lines = 10
        ).let { Result.success(it) }
    }

    override suspend fun createFreespin(command: CreateFreenspinCommand): Result<Unit> {
        val mainPreset = getPreset(gameSymbol = command.gameSymbol).getOrElse {
            return Result.failure(it)
        }

        val preset = (command.preset as? OneGameHubPreset) ?: return Result.failure(InvalidatePresetError())

        if (preset.quantity < mainPreset.minQuantity) {
            return Result.failure(InvalidatePresetError())
        }

        if (preset.betAmount < mainPreset.minBetAmount) {
            return Result.failure(InvalidatePresetError())
        }

        val payload = CreateFreespinDto(
            id = command.referenceId,

            startAt = command.startAt,
            endAt = command.endAt,

            number = preset.quantity,

            playerId = command.playerId,

            currency = command.currency.value,

            gameId = command.gameSymbol,

            bet = preset.betAmount,

            lineNumber = preset.lines
        )

        val response = client.post(addressUrl) {
            setAction("freerounds_create")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        if (!response.status.isSuccess()) {
            return Result.failure(Exception("Failed to create freespin from OneGameHub: ${response.status}"))
        }

        val responseBody = response.body<ResponseDto<String>>()

        if (!responseBody.success) {
            return Result.failure(Exception("Request isn't success. status: ${responseBody.status}"))
        }

        return Result.success(Unit)
    }

    override suspend fun cancelFreespin(commad: CancelFreespinCommand): Result<Unit> {
        val payload = CancelFreespinDto(id = commad.referenceId)

        val response = client.post(addressUrl) {
            setAction("freerounds_cancel")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        if (!response.status.isSuccess()) {
            return Result.failure(Exception("Failed to cancel freespin from OneGameHub: ${response.status}"))
        }

        val responseBody = response.body<ResponseDto<String>>()

        if (!responseBody.success) {
            return Result.failure(Exception("Request isn't success. status: ${responseBody.status}"))
        }

        return Result.success(Unit)
    }

    override suspend fun createLaunchUrl(command: CreateLaunchUrlCommand): Result<String> {
        val response = client.get(addressUrl) {
            setAction(if (command.isDemo) "demo_play" else "real_play")

            parameter("game_id", command.gameSymbol)

            if (!command.isDemo) {
                parameter("player_id", command.playerId)
            }

            parameter("currency", command.currency.value)
            parameter("mobile", if (command.platform == Platform.MOBILE) "1" else "0")
            parameter("language", command.locale.value)

            if (command.sessionToken.isNotBlank()) {
                parameter("extra", command.sessionToken)
            }

            parameter("return_url", command.lobbyUrl)
            parameter("deposit_url", command.lobbyUrl)
        }

        if (!response.status.isSuccess()) {
            return Result.failure(Exception("Failed to create launch url from OneGameHub: ${response.status}"))
        }

        val responseBody = response.body<ResponseDto<GameUrlDto>>()

        if (!responseBody.success || responseBody.response == null) {
            return Result.failure(Exception("Request isn't success. status: ${responseBody.status}"))

        }

        return Result.success(responseBody.response.gameUrl)
    }

    private fun HttpRequestBuilder.setAction(action: String) {
        parameter("action", action)
        parameter("secret", config.secret)
    }
}


