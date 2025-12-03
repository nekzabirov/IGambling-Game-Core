package infrastructure.aggregator.pateplay.client

import domain.common.error.AggregatorError
import infrastructure.aggregator.pateplay.client.dto.CancelFreespinBodyDto
import infrastructure.aggregator.pateplay.client.dto.CancelFreespinRequestDto
import infrastructure.aggregator.pateplay.client.dto.CreateFreespinBodyDto
import infrastructure.aggregator.pateplay.client.dto.CreateFreespinRequestDto
import infrastructure.aggregator.pateplay.client.dto.FreespinBonusDto
import infrastructure.aggregator.pateplay.client.dto.FreespinConfigDto
import infrastructure.aggregator.pateplay.client.dto.PateplayResponseDto
import infrastructure.aggregator.pateplay.model.PateplayConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal class PateplayHttpClient(private val config: PateplayConfig) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
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

    private val gatewayBaseUrl: String
        get() = "https://${config.gatewayUrl}"

    /**
     * Create free spins bonus for a player.
     * POST /bonuses/create
     *
     * @param payload Freespin creation parameters
     * @return Result indicating success or failure
     */
    suspend fun createFreespin(payload: CreateFreespinRequestDto): Result<Unit> {
        val body = CreateFreespinBodyDto(
            bonuses = listOf(
                FreespinBonusDto(
                    bonusRef = payload.referenceId,
                    playerId = payload.playerId,
                    siteCode = config.siteCode,
                    currency = payload.currency,
                    type = "bets",
                    config = FreespinConfigDto(
                        ttl = payload.ttlSeconds,
                        games = listOf(payload.gameSymbol),
                        stake = payload.stake,
                        bets = payload.rounds
                    ),
                    timeExpires = payload.expiresAt
                )
            )
        )

        return postGateway("/bonuses/create", body)
    }

    /**
     * Cancel free spins bonus.
     * POST /bonuses/cancel
     *
     * @param payload Freespin cancellation parameters
     * @return Result indicating success or failure
     */
    suspend fun cancelFreespin(payload: CancelFreespinRequestDto): Result<Unit> {
        val body = CancelFreespinBodyDto(
            ids = listOf(payload.bonusId),
            reason = payload.reason,
            force = payload.force
        )

        return postGateway("/bonuses/cancel", body)
    }

    /**
     * Generic POST request to PatePlay gateway with HMAC authentication.
     *
     * @param path API endpoint path
     * @param payload Request body to serialize and send
     * @return Result indicating success or failure
     */
    private suspend inline fun <reified T> postGateway(path: String, payload: T): Result<Unit> {
        val jsonBody = json.encodeToString(payload)
        val hmac = computeHmacSha256(jsonBody, config.gatewayApiSecret)

        return try {
            val response = client.post("$gatewayBaseUrl$path") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                header("x-api-key", config.gatewayApiKey)
                header("x-api-hmac", hmac)
                setBody(jsonBody)
            }

            if (!response.status.isSuccess()) {
                return Result.failure(
                    AggregatorError("PatePlay request failed with status: ${response.status}")
                )
            }

            val responseBody: PateplayResponseDto = response.body()

            if (!responseBody.isSuccess) {
                val error = responseBody.error
                return Result.failure(
                    AggregatorError("PatePlay API error: ${error?.code} - ${error?.message}")
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AggregatorError("PatePlay request failed: ${e.message}"))
        }
    }

    /**
     * Compute HMAC-SHA256 signature for PatePlay API authentication.
     *
     * @param data The JSON body string to sign
     * @param secret The API secret key
     * @return Lowercase hexadecimal string of the HMAC digest
     */
    private fun computeHmacSha256(data: String, secret: String): String {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), algorithm)
        mac.init(secretKeySpec)
        val hmacBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hmacBytes.joinToString("") { "%02x".format(it) }
    }
}
