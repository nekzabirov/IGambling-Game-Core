package application.port.inbound.command

import application.port.inbound.Command

data class RemoveGameTagCommand(
    val identity: String,
    val tag: String
) : Command<Unit>
