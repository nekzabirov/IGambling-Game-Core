package com.nekgamebling.infrastructure.player

import application.port.outbound.PlayerAdapter
import com.nekgamebling.infrastructure.player.dto.PlayerLimitDto
import com.nekgamebling.infrastructure.player.dto.PlayerResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.math.BigInteger
import kotlin.text.get

class TurboPlayerAdapter : PlayerAdapter {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 30000
        }

        install(Logging) {
            logger = Logger.Companion.DEFAULT
            level = LogLevel.ALL
        }
    }

    private val urlAddress by lazy {
        System.getenv()["TURBO_PLAYER_URL"] ?: "http://localhost:8080"
    }

    override suspend fun findCurrentBetLimit(playerId: String): Result<BigInteger?> = runCatching {
        val response: PlayerResponse<List<PlayerLimitDto>> =
            client.get("$urlAddress/api/player/limits/$playerId").body()

        if (response.data == null) throw Exception("Failed to fetch limits from TurboPlayer")

        val amount = response.data.find { it.isActive() && it.isPlaceBet() }
            ?.getRestAmount()
            ?: return Result.success(null)

        return Result.success(amount.toBigInteger())
    }
}