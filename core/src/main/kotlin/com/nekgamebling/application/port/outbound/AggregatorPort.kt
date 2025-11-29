package com.nekgamebling.application.port.outbound

import com.nekgamebling.domain.aggregator.model.AggregatorGame
import com.nekgamebling.domain.aggregator.model.AggregatorInfo
import com.nekgamebling.shared.value.Aggregator
import com.nekgamebling.shared.value.Currency
import com.nekgamebling.shared.value.Locale
import com.nekgamebling.shared.value.Platform
import kotlinx.datetime.LocalDateTime

/**
 * Port for getting game launch URLs from aggregators.
 */
interface AggregatorLaunchUrlPort {
    /**
     * Get launch URL for a game.
     */
    suspend fun getLaunchUrl(
        gameSymbol: String,
        sessionToken: String,
        playerId: String,
        locale: Locale,
        platform: Platform,
        currency: Currency,
        lobbyUrl: String,
        demo: Boolean
    ): Result<String>
}

/**
 * Port for managing freespins with aggregators.
 */
interface AggregatorFreespinPort {
    /**
     * Get freespin preset configuration for a game.
     */
    suspend fun getPreset(gameSymbol: String, aggregatorIdentity: String): Result<Map<String, Any?>>

    /**
     * Create a freespin for a player.
     */
    suspend fun createFreespin(
        aggregatorIdentity: String,
        presetValue: Map<String, String>,
        referenceId: String,
        playerId: String,
        gameSymbol: String,
        currency: Currency,
        startAt: LocalDateTime,
        endAt: LocalDateTime
    ): Result<Unit>

    /**
     * Cancel an existing freespin.
     */
    suspend fun cancelFreespin(
        aggregatorIdentity: String,
        referenceId: String,
        gameSymbol: String
    ): Result<Unit>
}

/**
 * Port for syncing games from aggregators.
 */
interface AggregatorGameSyncPort {
    /**
     * List all games available from an aggregator.
     */
    suspend fun listGames(): Result<List<AggregatorGame>>
}

/**
 * Factory for creating aggregator-specific adapters.
 * Implementations should return the appropriate adapter based on aggregator type.
 */
interface AggregatorAdapterFactory {
    /**
     * Check if this factory supports the given aggregator type.
     */
    fun supports(aggregator: Aggregator): Boolean

    /**
     * Create launch URL adapter for the aggregator.
     */
    fun createLaunchUrlAdapter(aggregatorInfo: AggregatorInfo): AggregatorLaunchUrlPort

    /**
     * Create freespin adapter for the aggregator.
     */
    fun createFreespinAdapter(aggregatorInfo: AggregatorInfo): AggregatorFreespinPort

    /**
     * Create game sync adapter for the aggregator.
     */
    fun createGameSyncAdapter(aggregatorInfo: AggregatorInfo): AggregatorGameSyncPort
}

/**
 * Registry for managing multiple aggregator adapter factories.
 */
interface AggregatorAdapterRegistry {
    /**
     * Get the factory for the given aggregator type.
     */
    fun getFactory(aggregator: Aggregator): AggregatorAdapterFactory?

    /**
     * Register a factory for an aggregator type.
     */
    fun register(factory: AggregatorAdapterFactory)
}
