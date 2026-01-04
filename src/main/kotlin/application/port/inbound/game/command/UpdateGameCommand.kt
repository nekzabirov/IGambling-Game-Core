package application.port.inbound.command

import application.port.inbound.Command

data class UpdateGameCommand(
    val identity: String,
    val bonusBetEnable: Boolean? = null,
    val bonusWageringEnable: Boolean? = null,
    val active: Boolean? = null
) : Command<Unit>
