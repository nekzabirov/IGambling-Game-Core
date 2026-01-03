package application.port.inbound.command

import application.port.inbound.Command
import application.port.outbound.MediaFile
import domain.provider.model.Provider
import java.util.UUID

/**
 * Command for updating a provider.
 */
data class UpdateProviderCommand(
    val identity: String,
    val order: Int? = null,
    val active: Boolean? = null
) : Command<Provider>

/**
 * Command for updating a provider image.
 */
data class UpdateProviderImageCommand(
    val identity: String,
    val key: String,
    val mediaFile: MediaFile
) : Command<Provider>

/**
 * Command for assigning a provider to an aggregator.
 */
data class AssignProviderToAggregatorCommand(
    val providerIdentity: String,
    val aggregatorIdentity: String
) : Command<Unit>
