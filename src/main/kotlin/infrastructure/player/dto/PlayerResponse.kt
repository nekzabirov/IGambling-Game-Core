package com.nekgamebling.infrastructure.player.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlayerResponse<T>(
    val data: T? = null
)
