package com.nekgamebling.application.port.inbound.collection.command

import application.port.inbound.Command

data class UpdateCollectionCommand(
    val identity: String,
    val active: Boolean? = null,
    val order: Int? = null
) : Command<Unit>
