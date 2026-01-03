package com.nekgamebling.application.port.inbound.game.query

import application.port.inbound.Query
import domain.common.value.Locale
import domain.common.value.Platform
import shared.value.Currency

data class GameDemoUrlQuery(
    val identity: String,
    val currency: Currency,
    val locale: Locale,
    val platform: Platform,
    val lobbyUrl: String
) : Query<GameDemoUrlResponse>

data class GameDemoUrlResponse(
    val launchUrl: String
)
