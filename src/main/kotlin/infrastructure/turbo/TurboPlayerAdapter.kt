package com.nekgamebling.infrastructure.turbo

import application.port.outbound.PlayerAdapter
import com.nekgamebling.infrastructure.turbo.dto.PlayerLimitDto
import com.nekgamebling.infrastructure.turbo.dto.TurboResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import java.math.BigInteger

class TurboPlayerAdapter : PlayerAdapter {

    private val client = TurboHttpClient.client

    private val urlAddress by lazy {
        System.getenv()["TURBO_PLAYER_URL"] ?: "http://localhost:8080"
    }

    override suspend fun findCurrentBetLimit(playerId: String): Result<BigInteger?> = runCatching {
        val response: TurboResponse<List<PlayerLimitDto>> =
            client.get("$urlAddress/limits/$playerId").body()

        if (response.data == null) throw Exception("Failed to fetch limits from TurboPlayer")

        val amount = response.data.find { it.isActive() && it.isPlaceBet() }
            ?.getRestAmount()
            ?: return Result.success(null)

        return Result.success(amount.toBigInteger())
    }
}
