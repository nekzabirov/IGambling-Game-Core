package application.port.inbound.command

import application.port.inbound.Command
import application.port.outbound.MediaFile
import domain.game.model.Game
import java.math.BigInteger

/**
 * Command for updating game properties.
 */
data class UpdateGameCommand(
    val identity: String,
    val name: String? = null,
    val bonusBetEnable: Boolean? = null,
    val bonusWageringEnable: Boolean? = null,
    val active: Boolean? = null
) : Command<Game>

/**
 * Command for updating a game image.
 */
data class UpdateGameImageCommand(
    val identity: String,
    val key: String,
    val mediaFile: MediaFile
) : Command<Game>

/**
 * Command for adding a tag to a game.
 */
data class AddGameTagCommand(
    val identity: String,
    val tag: String
) : Command<Unit>

/**
 * Command for removing a tag from a game.
 */
data class RemoveGameTagCommand(
    val identity: String,
    val tag: String
) : Command<Unit>

/**
 * Command for adding a game to player's favorites.
 */
data class AddGameFavouriteCommand(
    val gameIdentity: String,
    val playerId: String
) : Command<Unit>

/**
 * Command for removing a game from player's favorites.
 */
data class RemoveGameFavouriteCommand(
    val gameIdentity: String,
    val playerId: String
) : Command<Unit>

/**
 * Command for recording a game win.
 */
data class AddGameWinCommand(
    val gameIdentity: String,
    val playerId: String,
    val amount: BigInteger,
    val currency: String
) : Command<Unit>
