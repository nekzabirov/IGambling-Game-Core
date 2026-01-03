package application.port.inbound.command

import application.port.inbound.Command
import domain.aggregator.model.AggregatorInfo
import domain.common.value.Aggregator

/**
 * Command for adding a new aggregator.
 */
data class AddAggregatorCommand(
    val identity: String,
    val aggregator: Aggregator,
    val config: Map<String, String>,
    val active: Boolean = true
) : Command<AggregatorInfo>
