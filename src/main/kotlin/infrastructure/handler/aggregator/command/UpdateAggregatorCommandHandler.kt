package com.nekgamebling.infrastructure.handler.aggregator.command

import application.port.inbound.CommandHandler
import com.nekgamebling.application.port.inbound.aggregator.UpdateAggregatorCommand
import domain.common.error.NotFoundError
import infrastructure.persistence.exposed.table.AggregatorInfoTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class UpdateAggregatorCommandHandler : CommandHandler<UpdateAggregatorCommand, Unit> {

    override suspend fun handle(command: UpdateAggregatorCommand): Result<Unit> = newSuspendedTransaction {
        val existingRow = AggregatorInfoTable
            .selectAll()
            .where { AggregatorInfoTable.identity eq command.identity }
            .firstOrNull()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Aggregator", command.identity))

        // Only update if there's something to update
        if (command.active != null || command.config != null) {
            AggregatorInfoTable.update({ AggregatorInfoTable.identity eq command.identity }) {
                command.active?.let { value -> it[active] = value }
                command.config?.let { newConfig ->
                    // Merge new config with existing config
                    val existingConfig = existingRow[AggregatorInfoTable.config]
                    it[config] = existingConfig + newConfig
                }
            }
        }

        Result.success(Unit)
    }
}
