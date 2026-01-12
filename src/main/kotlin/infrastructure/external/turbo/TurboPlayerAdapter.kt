package infrastructure.external.turbo

import application.port.outbound.PlayerAdapter
import infrastructure.external.turbo.dto.PlayerLimitDto
import infrastructure.external.turbo.dto.TurboResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import shared.Logger
import java.util.concurrent.ConcurrentHashMap

class TurboPlayerAdapter : PlayerAdapter {

    private val client = TurboHttpClient.client

    private val urlAddress by lazy {
        System.getenv()["TURBO_PLAYER_URL"] ?: "http://localhost:8080"
    }

    // Cache player limits for 60 seconds (limits rarely change during a session)
    private data class CachedLimit(val limit: Long?, val expiresAt: Long)
    private val limitCache = ConcurrentHashMap<String, CachedLimit>()
    private val cacheTtlMs = 60_000L // 1 minute

    override suspend fun findCurrentBetLimit(playerId: String): Result<Long?> {
        // Check cache first
        val cached = limitCache[playerId]
        if (cached != null && System.currentTimeMillis() < cached.expiresAt) {
            Logger.info("[CACHE HIT] playerLimit for player=$playerId")
            return Result.success(cached.limit)
        }

        return runCatching {
            val response: TurboResponse<List<PlayerLimitDto>> = Logger.profileSuspend("turbo.player.findLimit") {
                client.get("$urlAddress/limits/$playerId").body()
            }

            if (response.data == null) throw Exception("Failed to fetch limits from TurboPlayer")

            val amount = response.data.find { it.isActive() && it.isPlaceBet() }
                ?.getRestAmount()

            // Cache the result
            limitCache[playerId] = CachedLimit(amount, System.currentTimeMillis() + cacheTtlMs)

            amount
        }
    }
}
