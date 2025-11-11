package com.nekzabirov.aggregators.core

import com.nekzabirov.aggregators.command.CancelFreespinCommand
import com.nekzabirov.aggregators.command.CreateFreenspinCommand
import com.nekzabirov.aggregators.command.CreateLaunchUrlCommand
import com.nekzabirov.aggregators.model.Game
import com.nekzabirov.aggregators.value.Aggregator

interface IAggregatorAdapter {
    val config: IAggregatorConfig

    val aggregator: Aggregator

    suspend fun listGames() : Result<List<Game>>

    suspend fun getPreset(gameSymbol: String) : Result<IAggregatorPreset>

    suspend fun createFreespin(command: CreateFreenspinCommand) : Result<Unit>

    suspend fun cancelFreespin(commad: CancelFreespinCommand) : Result<Unit>

    suspend fun createLaunchUrl(command: CreateLaunchUrlCommand) : Result<String>
}