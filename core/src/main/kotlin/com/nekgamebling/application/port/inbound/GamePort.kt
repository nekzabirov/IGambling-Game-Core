package com.nekgamebling.application.port.inbound

import com.nekgamebling.domain.aggregator.model.AggregatorInfo
import com.nekgamebling.domain.game.model.GameVariant

interface GamePort {
    suspend fun syncGame(variants: List<GameVariant>, aggregatorInfo: AggregatorInfo)
}