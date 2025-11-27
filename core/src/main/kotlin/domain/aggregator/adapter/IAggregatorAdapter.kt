package domain.aggregator.adapter

import domain.aggregator.adapter.command.CancelFreespinCommand
import domain.aggregator.adapter.command.CreateFreenspinCommand
import domain.aggregator.adapter.command.CreateLaunchUrlCommand
import domain.aggregator.model.AggregatorGame
import domain.aggregator.model.Aggregator

interface IAggregatorAdapter {
    val config: IAggregatorConfig

    val aggregator: Aggregator

    suspend fun listGames() : Result<List<AggregatorGame>>

    suspend fun getPreset(gameSymbol: String) : Result<IAggregatorPreset>

    suspend fun createFreespin(command: CreateFreenspinCommand) : Result<Unit>

    suspend fun cancelFreespin(commad: CancelFreespinCommand) : Result<Unit>

    suspend fun createLaunchUrl(command: CreateLaunchUrlCommand) : Result<String>
}