package com.nekgamebling.infrastructure.aggregator.onegamehub

internal class OneGameHubConfig(private val config: Map<String, String>) {
    val salt = config["salt"] ?: ""

    val secret = config["secret"] ?: ""

    val partner = config["partner"] ?: ""

    val gateway = config["gateway"] ?: ""
}