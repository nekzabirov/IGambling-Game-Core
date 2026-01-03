package application.port.inbound.command

import application.port.inbound.Command
import domain.collection.model.Collection
import shared.value.LocaleName

/**
 * Command for adding a new collection.
 */
data class AddCollectionCommand(
    val identity: String,
    val name: LocaleName
) : Command<Collection>

/**
 * Command for updating a collection.
 */
data class UpdateCollectionCommand(
    val identity: String,
    val name: LocaleName? = null,
    val order: Int? = null,
    val active: Boolean? = null
) : Command<Collection>

/**
 * Command for adding a game to a collection.
 */
data class AddGameToCollectionCommand(
    val collectionIdentity: String,
    val gameIdentity: String
) : Command<Unit>

/**
 * Command for removing a game from a collection.
 */
data class RemoveGameFromCollectionCommand(
    val collectionIdentity: String,
    val gameIdentity: String
) : Command<Unit>

/**
 * Command for changing game order in a collection.
 */
data class ChangeGameOrderInCollectionCommand(
    val collectionIdentity: String,
    val gameIdentity: String,
    val newOrder: Int
) : Command<Unit>
