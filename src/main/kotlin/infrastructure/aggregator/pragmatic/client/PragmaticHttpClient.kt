package infrastructure.aggregator.pragmatic.client

import domain.common.error.AggregatorError
import infrastructure.aggregator.pragmatic.model.PragmaticConfig
import infrastructure.aggregator.pragmatic.client.dto.CreateFreespinDto
import infrastructure.aggregator.pragmatic.client.dto.FreespinBetValueDto
import infrastructure.aggregator.pragmatic.client.dto.FreespinGameDto
import infrastructure.aggregator.pragmatic.client.dto.FreespinGameListDto
import infrastructure.aggregator.pragmatic.client.dto.GameDto
import infrastructure.aggregator.pragmatic.client.dto.GameUrlResponseDto
import infrastructure.aggregator.pragmatic.client.dto.GamesResponseDto
import infrastructure.aggregator.pragmatic.client.dto.LaunchUrlRequestDto
import infrastructure.aggregator.pragmatic.client.dto.ResponseDto
import shared.value.Platform
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

internal class PragmaticHttpClient(private val config: PragmaticConfig) {

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

    private val baseUrl: String
        get() = "https://${config.gateWayUrl}"

    /**
     * Fetch list of available casino games from Pragmatic Play.
     * API: POST /IntegrationService/v3/http/CasinoGameAPI/getCasinoGames
     */
    suspend fun listGames(): Result<List<GameDto>> {
        val params = mutableMapOf(
            "secureLogin" to config.secureLogin,
            "options" to "GetFrbDetails,GetLines,GetDataTypes,GetFeatures,GetFcDetails,GetStudio"
        )
        params["hash"] = generateHash(params)

        return try {
            val response = client.post("$baseUrl/IntegrationService/v3/http/CasinoGameAPI/getCasinoGames") {
                contentType(ContentType.Application.FormUrlEncoded)
                accept(ContentType.Application.Json)
                setBody(FormDataContent(Parameters.build {
                    params.forEach { (key, value) -> append(key, value) }
                }))
            }

            if (!response.status.isSuccess()) {
                return Result.failure(AggregatorError("Failed to fetch games from Pragmatic: ${response.status}"))
            }

            val body: GamesResponseDto = response.body()

            if (!body.success) {
                return Result.failure(AggregatorError("Pragmatic API error: ${body.error} - ${body.description}"))
            }

            Result.success(body.gameList ?: emptyList())
        } catch (e: Exception) {
            Result.failure(AggregatorError("Failed to fetch games from Pragmatic: ${e.message}"))
        }
    }

    /**
     * Get game launch URL from Pragmatic Play.
     * API: POST /IntegrationService/v3/http/CasinoGameAPI/game/url/
     */
    suspend fun getLaunchUrl(payload: LaunchUrlRequestDto): Result<String> {
        val params = mutableMapOf(
            "secureLogin" to config.secureLogin,
            "externalPlayerId" to payload.playerId,
            "token" to payload.sessionToken,
            "language" to payload.locale,
            "symbol" to payload.gameSymbol,
            "currency" to payload.currency,
            "platform" to payload.platform.toPragmaticPlatform(),
            "playMode" to if (payload.demo) "DEMO" else "REAL",
            "lobbyUrl" to payload.lobbyUrl,
            "cashierUrl" to payload.lobbyUrl
        )
        params["hash"] = generateHash(params)

        return try {
            val response = client.post("$baseUrl/IntegrationService/v3/http/CasinoGameAPI/game/url/") {
                contentType(ContentType.Application.FormUrlEncoded)
                accept(ContentType.Application.Json)
                setBody(FormDataContent(Parameters.build {
                    params.forEach { (key, value) -> append(key, value) }
                }))
            }

            if (!response.status.isSuccess()) {
                return Result.failure(AggregatorError("Failed to get launch URL from Pragmatic: ${response.status}"))
            }

            val body: GameUrlResponseDto = response.body()

            if (!body.success) {
                return Result.failure(AggregatorError("Pragmatic API error: ${body.error} - ${body.description}"))
            }

            if (body.gameURL.isNullOrBlank()) {
                return Result.failure(AggregatorError("Pragmatic returned empty game URL"))
            }

            Result.success(body.gameURL)
        } catch (e: Exception) {
            Result.failure(AggregatorError("Failed to get launch URL from Pragmatic: ${e.message}"))
        }
    }

    /**
     * Create free spins bonus for a player.
     * API: POST /IntegrationService/v3/http/FreeRoundsBonusAPI/v2/bonus/player/create
     * Parameters go in query string, JSON body contains game list.
     */
    suspend fun createFreespin(payload: CreateFreespinDto): Result<Unit> {
        val params = mutableMapOf(
            "secureLogin" to config.secureLogin,
            "bonusCode" to payload.bonusCode,
            "playerId" to payload.playerId,
            "currency" to payload.currency,
            "rounds" to payload.rounds.toString(),
            "startDate" to payload.startTimestamp.toString(),
            "expirationDate" to payload.expirationTimestamp.toString()
        )
        params["hash"] = generateHash(params)

        val jsonBody = FreespinGameListDto(
            gameList = listOf(
                FreespinGameDto(
                    gameId = payload.gameSymbol,
                    betValues = listOf(
                        FreespinBetValueDto(
                            currency = payload.currency,
                            totalBet = payload.totalBet
                        )
                    )
                )
            )
        )

        return try {
            val url = buildString {
                append("$baseUrl/IntegrationService/v3/http/FreeRoundsBonusAPI/v2/bonus/player/create")
                append("?")
                append(params.entries.joinToString("&") { "${it.key}=${it.value}" })
            }

            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(json.encodeToString(jsonBody))
            }

            if (!response.status.isSuccess()) {
                return Result.failure(AggregatorError("Failed to create freespin in Pragmatic: ${response.status}"))
            }

            val body: ResponseDto = response.body()

            if (!body.success) {
                return Result.failure(AggregatorError("Pragmatic API error: ${body.error} - ${body.description}"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AggregatorError("Failed to create freespin in Pragmatic: ${e.message}"))
        }
    }

    /**
     * Cancel free spins bonus.
     * API: POST /IntegrationService/v3/http/FreeRoundsBonusAPI/v2/bonus/cancel/
     */
    suspend fun cancelFreespin(bonusCode: String): Result<Unit> {
        val params = mutableMapOf(
            "secureLogin" to config.secureLogin,
            "bonusCode" to bonusCode
        )
        params["hash"] = generateHash(params)

        return try {
            val response = client.post("$baseUrl/IntegrationService/v3/http/FreeRoundsBonusAPI/v2/bonus/cancel/") {
                contentType(ContentType.Application.FormUrlEncoded)
                accept(ContentType.Application.Json)
                setBody(FormDataContent(Parameters.build {
                    params.forEach { (key, value) -> append(key, value) }
                }))
            }

            if (!response.status.isSuccess()) {
                return Result.failure(AggregatorError("Failed to cancel freespin in Pragmatic: ${response.status}"))
            }

            val body: ResponseDto = response.body()

            if (!body.success) {
                return Result.failure(AggregatorError("Pragmatic API error: ${body.error} - ${body.description}"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AggregatorError("Failed to cancel freespin in Pragmatic: ${e.message}"))
        }
    }

    /**
     * Generate MD5 hash for Pragmatic API authentication.
     *
     * Algorithm:
     * 1. Sort parameters alphabetically by key
     * 2. Build query string: key1=value1&key2=value2&...
     * 3. Append secretKey directly (no separator)
     * 4. Calculate MD5 hash
     * 5. Return lowercase hexadecimal string
     */
    private fun generateHash(params: Map<String, String>): String {
        val sortedParams = params.toSortedMap()
        val queryString = sortedParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        val dataToHash = queryString + config.secretKey

        return MessageDigest.getInstance("MD5")
            .digest(dataToHash.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * Convert platform to Pragmatic format.
     */
    private fun Platform.toPragmaticPlatform(): String = when (this) {
        Platform.DESKTOP -> "DESKTOP"
        Platform.MOBILE -> "MOBILE"
        Platform.DOWNLOAD -> "DOWNLOAD"
    }
}
