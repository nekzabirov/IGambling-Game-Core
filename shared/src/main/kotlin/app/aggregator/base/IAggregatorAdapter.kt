package app.aggregator.base

import app.aggregator.base.command.CancelFreespinCommand
import app.aggregator.base.command.CreateFreenspinCommand
import app.aggregator.base.command.CreateLaunchUrlCommand
import domain.model.AggregatorGame
import domain.value.Aggregator

interface IAggregatorAdapter {
    val config: IAggregatorConfig

    val aggregator: Aggregator

    suspend fun listGames() : Result<List<AggregatorGame>>

    suspend fun getPreset(gameSymbol: String) : Result<IAggregatorPreset>

    suspend fun createFreespin(command: CreateFreenspinCommand) : Result<Unit>

    suspend fun cancelFreespin(commad: CancelFreespinCommand) : Result<Unit>

    suspend fun createLaunchUrl(command: CreateLaunchUrlCommand) : Result<String>
}