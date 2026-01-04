package infrastructure.handler

import application.port.inbound.CommandHandler
import application.port.inbound.command.UpdateGameCommand
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.table.GameTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class UpdateGameCommandHandler : CommandHandler<UpdateGameCommand, Unit> {
    override suspend fun handle(command: UpdateGameCommand): Result<Unit> = newSuspendedTransaction {
        // Check if game exists
        val exists = GameTable
            .selectAll()
            .where { GameTable.identity eq command.identity }
            .count() > 0

        if (!exists) {
            return@newSuspendedTransaction Result.failure(NotFoundError("Game", command.identity))
        }

        // Update only provided fields
        GameTable.update({ GameTable.identity eq command.identity }) {
            command.bonusBetEnable?.let { value -> it[bonusBetEnable] = value }
            command.bonusWageringEnable?.let { value -> it[bonusWageringEnable] = value }
            command.active?.let { value -> it[active] = value }
        }

        Result.success(Unit)
    }
}
