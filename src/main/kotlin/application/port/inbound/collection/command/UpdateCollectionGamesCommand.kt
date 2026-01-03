package com.nekgamebling.application.port.inbound.collection.command

import application.port.inbound.Command

data class UpdateCollectionGamesCommand(
    val identity: String,
    val addGames: List<String> = emptyList(),
    val removeGames: List<String> = emptyList()
) : Command<Unit>
