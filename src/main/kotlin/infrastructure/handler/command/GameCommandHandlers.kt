package infrastructure.handler.command

import application.port.inbound.CommandHandler
import application.port.inbound.command.*
import application.port.outbound.EventPublisherAdapter
import application.port.outbound.FileAdapter
import domain.common.error.NotFoundError
import domain.common.event.GameFavouriteAddedEvent
import domain.common.event.GameFavouriteRemovedEvent
import domain.common.event.GameWonEvent
import domain.game.model.Game
import infrastructure.persistence.exposed.mapper.toGame
import infrastructure.persistence.exposed.table.GameFavouriteTable
import infrastructure.persistence.exposed.table.GameTable
import infrastructure.persistence.exposed.table.GameWonTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import shared.value.Currency
import shared.value.ImageMap
import java.util.UUID

/**
 * Command handler for updating game properties.
 */
class UpdateGameCommandHandler : CommandHandler<UpdateGameCommand, Game> {
    override suspend fun handle(command: UpdateGameCommand): Result<Game> = newSuspendedTransaction {
        val existingGame = GameTable.selectAll()
            .where { GameTable.identity eq command.identity }
            .singleOrNull()
            ?.toGame()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Game", command.identity))

        val updatedGame = existingGame.copy(
            name = command.name ?: existingGame.name,
            bonusBetEnable = command.bonusBetEnable ?: existingGame.bonusBetEnable,
            bonusWageringEnable = command.bonusWageringEnable ?: existingGame.bonusWageringEnable,
            active = command.active ?: existingGame.active
        )

        GameTable.update({ GameTable.id eq existingGame.id }) {
            it[name] = updatedGame.name
            it[bonusBetEnable] = updatedGame.bonusBetEnable
            it[bonusWageringEnable] = updatedGame.bonusWageringEnable
            it[active] = updatedGame.active
        }

        Result.success(updatedGame)
    }
}

/**
 * Command handler for updating a game image.
 */
class UpdateGameImageCommandHandler(
    private val fileAdapter: FileAdapter
) : CommandHandler<UpdateGameImageCommand, Game> {
    override suspend fun handle(command: UpdateGameImageCommand): Result<Game> {
        val game = newSuspendedTransaction {
            GameTable.selectAll()
                .where { GameTable.identity eq command.identity }
                .singleOrNull()
                ?.toGame()
        } ?: return Result.failure(NotFoundError("Game", command.identity))

        val uploadResult = fileAdapter.upload(
            folder = "games/${game.identity}",
            fileName = command.key,
            file = command.mediaFile
        )

        val path = uploadResult.getOrElse {
            return Result.failure(it)
        }

        val updatedImages = game.images.data.toMutableMap()
        updatedImages[command.key] = path
        val newImageMap = ImageMap(updatedImages)

        return newSuspendedTransaction {
            GameTable.update({ GameTable.id eq game.id }) {
                it[images] = newImageMap
            }
            Result.success(game.copy(images = newImageMap))
        }
    }
}

/**
 * Command handler for adding a tag to a game.
 */
class AddGameTagCommandHandler : CommandHandler<AddGameTagCommand, Unit> {
    override suspend fun handle(command: AddGameTagCommand): Result<Unit> = newSuspendedTransaction {
        val game = GameTable.selectAll()
            .where { GameTable.identity eq command.identity }
            .singleOrNull()
            ?.toGame()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Game", command.identity))

        val updatedTags = game.tags.toMutableList()
        if (!updatedTags.contains(command.tag)) {
            updatedTags.add(command.tag)
        }

        GameTable.update({ GameTable.id eq game.id }) {
            it[tags] = updatedTags
        }

        Result.success(Unit)
    }
}

/**
 * Command handler for removing a tag from a game.
 */
class RemoveGameTagCommandHandler : CommandHandler<RemoveGameTagCommand, Unit> {
    override suspend fun handle(command: RemoveGameTagCommand): Result<Unit> = newSuspendedTransaction {
        val game = GameTable.selectAll()
            .where { GameTable.identity eq command.identity }
            .singleOrNull()
            ?.toGame()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Game", command.identity))

        val updatedTags = game.tags.toMutableList()
        updatedTags.remove(command.tag)

        GameTable.update({ GameTable.id eq game.id }) {
            it[tags] = updatedTags
        }

        Result.success(Unit)
    }
}

/**
 * Command handler for adding a game to player's favorites.
 */
class AddGameFavouriteCommandHandler(
    private val eventPublisher: EventPublisherAdapter
) : CommandHandler<AddGameFavouriteCommand, Unit> {
    override suspend fun handle(command: AddGameFavouriteCommand): Result<Unit> {
        val game = newSuspendedTransaction {
            GameTable.selectAll()
                .where { GameTable.identity eq command.gameIdentity }
                .singleOrNull()
                ?.toGame()
        } ?: return Result.failure(NotFoundError("Game", command.gameIdentity))

        newSuspendedTransaction {
            // Check if already exists
            val exists = GameFavouriteTable.selectAll()
                .where { (GameFavouriteTable.playerId eq command.playerId) and (GameFavouriteTable.gameId eq game.id) }
                .count() > 0

            if (!exists) {
                GameFavouriteTable.insert {
                    it[playerId] = command.playerId
                    it[gameId] = game.id
                }
            }
        }

        eventPublisher.publish(
            GameFavouriteAddedEvent(
                gameId = game.id.toString(),
                gameIdentity = game.identity,
                playerId = command.playerId
            )
        )

        return Result.success(Unit)
    }
}

/**
 * Command handler for removing a game from player's favorites.
 */
class RemoveGameFavouriteCommandHandler(
    private val eventPublisher: EventPublisherAdapter
) : CommandHandler<RemoveGameFavouriteCommand, Unit> {
    override suspend fun handle(command: RemoveGameFavouriteCommand): Result<Unit> {
        val game = newSuspendedTransaction {
            GameTable.selectAll()
                .where { GameTable.identity eq command.gameIdentity }
                .singleOrNull()
                ?.toGame()
        } ?: return Result.failure(NotFoundError("Game", command.gameIdentity))

        newSuspendedTransaction {
            GameFavouriteTable.deleteWhere {
                (GameFavouriteTable.playerId eq command.playerId) and (GameFavouriteTable.gameId eq game.id)
            }
        }

        eventPublisher.publish(
            GameFavouriteRemovedEvent(
                gameId = game.id.toString(),
                gameIdentity = game.identity,
                playerId = command.playerId
            )
        )

        return Result.success(Unit)
    }
}

/**
 * Command handler for recording a game win.
 */
class AddGameWinCommandHandler(
    private val eventPublisher: EventPublisherAdapter
) : CommandHandler<AddGameWinCommand, Unit> {
    override suspend fun handle(command: AddGameWinCommand): Result<Unit> {
        val game = newSuspendedTransaction {
            GameTable.selectAll()
                .where { GameTable.identity eq command.gameIdentity }
                .singleOrNull()
                ?.toGame()
        } ?: return Result.failure(NotFoundError("Game", command.gameIdentity))

        newSuspendedTransaction {
            GameWonTable.insert {
                it[gameId] = game.id
                it[playerId] = command.playerId
                it[amount] = command.amount.toLong()
                it[currency] = command.currency
            }
        }

        eventPublisher.publish(
            GameWonEvent(
                gameId = game.id.toString(),
                gameIdentity = game.identity,
                playerId = command.playerId,
                amount = command.amount,
                currency = Currency(command.currency)
            )
        )

        return Result.success(Unit)
    }
}
