package com.nekgamebling.application.port.inbound.collection.command

import application.port.inbound.Command
import domain.collection.model.Collection
import shared.value.LocaleName

data class CreateCollectionCommand(
    val identity: String,
    val name: LocaleName,
    val active: Boolean = true,
    val order: Int = 100
) : Command<CreateCollectionResponse>

data class CreateCollectionResponse(
    val collection: Collection
)
