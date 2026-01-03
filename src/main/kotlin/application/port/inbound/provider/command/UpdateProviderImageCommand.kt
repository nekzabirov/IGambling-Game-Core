package com.nekgamebling.application.port.inbound.provider.command

import application.port.inbound.Command

data class UpdateProviderImageCommand(
    val identity: String,
    val key: String,
    val file: ByteArray,
    val extension: String
) : Command<Unit>
