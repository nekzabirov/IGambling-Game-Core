package com.nekgamebling.application.port.inbound.aggregator

import application.port.inbound.Command

data class UpdateAggregatorCommand(
    val identity: String,
    val active: Boolean? = null,
    val config: Map<String, String>? = null
) : Command<Unit>
