package com.nekgamebling.infrastructure.handler.provider.command

import application.port.inbound.CommandHandler
import com.nekgamebling.application.port.inbound.provider.command.UpdateProviderCommand
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.table.ProviderTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class UpdateProviderCommandHandler : CommandHandler<UpdateProviderCommand, Unit> {

    override suspend fun handle(command: UpdateProviderCommand): Result<Unit> = newSuspendedTransaction {
        val exists = ProviderTable
            .selectAll()
            .where { ProviderTable.identity eq command.identity }
            .count() > 0

        if (!exists) {
            return@newSuspendedTransaction Result.failure(NotFoundError("Provider", command.identity))
        }

        // Only update if there's something to update
        if (command.active != null || command.order != null) {
            ProviderTable.update({ ProviderTable.identity eq command.identity }) {
                command.active?.let { value -> it[active] = value }
                command.order?.let { value -> it[order] = value }
            }
        }

        Result.success(Unit)
    }
}
