package app.aggregator.base.command

import domain.value.Currency
import domain.value.Locale
import domain.value.Platform

data class CreateLaunchUrlCommand(
    val gameSymbol: String,

    val playerId: String,

    val sessionToken: String,

    val lobbyUrl: String,

    val locale: Locale,

    val currency: Currency,

    val platform: Platform,

    val isDemo: Boolean
)
