package domain.aggregator.adapter.command

import core.value.Currency
import core.value.Locale
import core.value.Platform

data class CreateLaunchUrlCommand(
    val gameSymbol: String,

    val playerId: String = "",

    val sessionToken: String = "",

    val lobbyUrl: String,

    val locale: Locale,

    val currency: Currency,

    val platform: Platform,

    val isDemo: Boolean
)
