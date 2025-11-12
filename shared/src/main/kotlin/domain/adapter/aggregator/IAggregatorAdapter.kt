package domain.adapter.aggregator

import domain.adapter.aggregator.command.CancelFreespinCommand
import domain.adapter.aggregator.command.CreateFreenspinCommand
import domain.adapter.aggregator.command.CreateLaunchUrlCommand
import domain.model.AggregatorGame
import domain.value.Aggregator

interface IAggregatorAdapter {
    val config: IAggregatorConfig

    val aggregator: domain.value.Aggregator

    suspend fun listGames() : Result<List<domain.model.AggregatorGame>>

    suspend fun getPreset(gameSymbol: String) : Result<IAggregatorPreset>

    suspend fun createFreespin(command: domain.adapter.aggregator.command.CreateFreenspinCommand) : Result<Unit>

    suspend fun cancelFreespin(commad: domain.adapter.aggregator.command.CancelFreespinCommand) : Result<Unit>

    suspend fun createLaunchUrl(command: domain.adapter.aggregator.command.CreateLaunchUrlCommand) : Result<String>
}