package com.nekzabirov.aggregators.command

import com.nekzabirov.aggregators.value.Currency
import com.nekzabirov.aggregators.value.Locale
import com.nekzabirov.aggregators.value.Platform

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
