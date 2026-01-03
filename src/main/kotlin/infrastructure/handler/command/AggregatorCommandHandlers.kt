package infrastructure.handler.command

import application.port.inbound.CommandHandler
import application.port.inbound.command.AddAggregatorCommand
import domain.aggregator.model.AggregatorInfo
import domain.common.error.DuplicateEntityError
import infrastructure.persistence.exposed.table.AggregatorInfoTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/**
 * Command handler for adding a new aggregator.
 */
class AddAggregatorCommandHandler : CommandHandler<AddAggregatorCommand, AggregatorInfo> {
    override suspend fun handle(command: AddAggregatorCommand): Result<AggregatorInfo> = newSuspendedTransaction {
        val exists = AggregatorInfoTable.selectAll()
            .where { AggregatorInfoTable.identity eq command.identity }
            .count() > 0

        if (exists) {
            return@newSuspendedTransaction Result.failure(DuplicateEntityError("Aggregator", command.identity))
        }

        val aggregatorInfo = AggregatorInfo(
            id = UUID.randomUUID(),
            identity = command.identity,
            config = command.config,
            aggregator = command.aggregator,
            active = command.active
        )

        val id = AggregatorInfoTable.insertAndGetId {
            it[identity] = aggregatorInfo.identity
            it[config] = aggregatorInfo.config
            it[aggregator] = aggregatorInfo.aggregator
            it[active] = aggregatorInfo.active
        }

        Result.success(aggregatorInfo.copy(id = id.value))
    }
}
