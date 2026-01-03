package infrastructure.handler.command

import application.port.inbound.CommandHandler
import application.port.inbound.command.*
import application.port.outbound.FileAdapter
import domain.common.error.NotFoundError
import domain.provider.model.Provider
import infrastructure.persistence.exposed.mapper.toAggregatorInfo
import infrastructure.persistence.exposed.mapper.toProvider
import infrastructure.persistence.exposed.table.AggregatorInfoTable
import infrastructure.persistence.exposed.table.ProviderTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import shared.value.ImageMap

/**
 * Command handler for updating a provider.
 */
class UpdateProviderCommandHandler : CommandHandler<UpdateProviderCommand, Provider> {
    override suspend fun handle(command: UpdateProviderCommand): Result<Provider> = newSuspendedTransaction {
        val existing = ProviderTable.selectAll()
            .where { ProviderTable.identity eq command.identity }
            .singleOrNull()
            ?.toProvider()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Provider", command.identity))

        val updated = existing.copy(
            order = command.order ?: existing.order,
            active = command.active ?: existing.active
        )

        ProviderTable.update({ ProviderTable.id eq existing.id }) {
            it[order] = updated.order
            it[active] = updated.active
        }

        Result.success(updated)
    }
}

/**
 * Command handler for updating a provider image.
 */
class UpdateProviderImageCommandHandler(
    private val fileAdapter: FileAdapter
) : CommandHandler<UpdateProviderImageCommand, Provider> {
    override suspend fun handle(command: UpdateProviderImageCommand): Result<Provider> {
        val provider = newSuspendedTransaction {
            ProviderTable.selectAll()
                .where { ProviderTable.identity eq command.identity }
                .singleOrNull()
                ?.toProvider()
        } ?: return Result.failure(NotFoundError("Provider", command.identity))

        val uploadResult = fileAdapter.upload(
            folder = "providers/${provider.identity}",
            fileName = command.key,
            file = command.mediaFile
        )

        val path = uploadResult.getOrElse {
            return Result.failure(it)
        }

        val updatedImagesData = provider.images.data.toMutableMap()
        updatedImagesData[command.key] = path
        val updatedImages = ImageMap(updatedImagesData)

        return newSuspendedTransaction {
            ProviderTable.update({ ProviderTable.id eq provider.id }) {
                it[images] = updatedImages
            }
            Result.success(provider.copy(images = updatedImages))
        }
    }
}

/**
 * Command handler for assigning a provider to an aggregator.
 */
class AssignProviderToAggregatorCommandHandler : CommandHandler<AssignProviderToAggregatorCommand, Unit> {
    override suspend fun handle(command: AssignProviderToAggregatorCommand): Result<Unit> = newSuspendedTransaction {
        val provider = ProviderTable.selectAll()
            .where { ProviderTable.identity eq command.providerIdentity }
            .singleOrNull()
            ?.toProvider()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Provider", command.providerIdentity))

        val aggregator = AggregatorInfoTable.selectAll()
            .where { AggregatorInfoTable.identity eq command.aggregatorIdentity }
            .singleOrNull()
            ?.toAggregatorInfo()
            ?: return@newSuspendedTransaction Result.failure(NotFoundError("Aggregator", command.aggregatorIdentity))

        ProviderTable.update({ ProviderTable.id eq provider.id }) {
            it[aggregatorId] = aggregator.id
        }

        Result.success(Unit)
    }
}
