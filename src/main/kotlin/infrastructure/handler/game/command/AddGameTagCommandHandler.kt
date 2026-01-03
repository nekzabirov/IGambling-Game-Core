package infrastructure.handler

import application.port.inbound.CommandHandler
import application.port.inbound.command.AddGameTagCommand
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.table.GameTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class AddGameTagCommandHandler : CommandHandler<AddGameTagCommand, Unit> {
    override suspend fun handle(command: AddGameTagCommand): Result<Unit> = newSuspendedTransaction {
        val currentTags = GameTable
            .selectAll()
            .where { GameTable.identity eq command.identity }
            .firstOrNull()
            ?.get(GameTable.tags)
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Game", command.identity))

        // Add tag if not already present
        if (command.tag !in currentTags) {
            GameTable.update({ GameTable.identity eq command.identity }) {
                it[tags] = currentTags + command.tag
            }
        }

        Result.success(Unit)
    }
}
