package com.nekgamebling.application.port.inbound.provider.command

import application.port.inbound.Command

data class UpdateProviderCommand(
    val identity: String,
    val active: Boolean? = null,
    val order: Int? = null
) : Command<Unit>
