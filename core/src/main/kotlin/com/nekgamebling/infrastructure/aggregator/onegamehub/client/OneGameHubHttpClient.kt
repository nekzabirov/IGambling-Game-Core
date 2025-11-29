package com.nekgamebling.infrastructure.aggregator.onegamehub.client

import com.nekgamebling.domain.common.error.AggregatorError
import com.nekgamebling.infrastructure.aggregator.onegamehub.OneGameHubConfig
import com.nekgamebling.infrastructure.aggregator.onegamehub.client.dto.GameDto
import com.nekgamebling.infrastructure.aggregator.onegamehub.client.dto.GameUrlDto
import com.nekgamebling.infrastructure.aggregator.onegamehub.client.dto.ResponseDto
import com.nekgamebling.shared.value.Currency
import com.nekgamebling.shared.value.Locale
import com.nekgamebling.shared.value.Platform
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

internal class OneGameHubHttpClient(private val config: OneGameHubConfig) {
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
            logger = Logger.Companion.DEFAULT
            level = LogLevel.ALL
        }
    }

    private val address = "https://${config.gateway}/integrations/${config.partner}/rpc"

    suspend fun listGames(): Result<ResponseDto<List<GameDto>>> {
        val response = client.get(address) {
            setAction("available_games")
        }

        if (!response.status.isSuccess()) {
            return Result.failure(AggregatorError("Failed to fetch games from OneGameHub: ${response.status}"))
        }

        return Result.success(response.body())
    }

    suspend fun getLaunchUrl(
        gameSymbol: String,
        sessionToken: String,
        playerId: String,
        locale: String,
        platform: Platform,
        currency: String,
        lobbyUrl: String,
        demo: Boolean
    ): Result<ResponseDto<GameUrlDto>> {
        val response = client.get(address) {
            setAction(if (demo) "demo_play" else "real_play")

            parameter("game_id", gameSymbol)

            if (!demo) {
                parameter("player_id", playerId)
            }

            parameter("currency", currency)
            parameter("mobile", if (platform == Platform.MOBILE) "1" else "0")
            parameter("language", locale)

            if (sessionToken.isNotBlank()) {
                parameter("extra", sessionToken)
            }

            parameter("return_url", lobbyUrl)
            parameter("deposit_url", lobbyUrl)
        }

        if (!response.status.isSuccess()) {
            return Result.failure(AggregatorError("Failed to fetch games from OneGameHub: ${response.status}"))
        }

        return Result.success(response.body())
    }

    private fun HttpRequestBuilder.setAction(action: String) {
        parameter("action", action)
        parameter("secret", config.secret)
    }
}