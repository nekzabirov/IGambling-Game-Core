package domain.common.value

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Base interface for type-safe entity identifiers.
 * Using value classes provides type safety without runtime overhead.
 *
 * Note: UUID-based EntityIds are not @Serializable - use string representations
 * in DTOs and convert via from()/toString().
 */
interface EntityId {
    val value: UUID

    companion object {
        fun generate(): UUID = UUID.randomUUID()
    }
}

/**
 * Session identifier.
 */
@JvmInline
value class SessionId(override val value: UUID) : EntityId {
    override fun toString() = value.toString()
    companion object {
        fun generate() = SessionId(EntityId.generate())
        fun from(value: String) = SessionId(UUID.fromString(value))
    }
}

/**
 * Round identifier.
 */
@JvmInline
value class RoundId(override val value: UUID) : EntityId {
    override fun toString() = value.toString()
    companion object {
        fun generate() = RoundId(EntityId.generate())
        fun from(value: String) = RoundId(UUID.fromString(value))
    }
}

/**
 * Spin identifier.
 */
@JvmInline
value class SpinId(override val value: UUID) : EntityId {
    override fun toString() = value.toString()
    companion object {
        fun generate() = SpinId(EntityId.generate())
        fun from(value: String) = SpinId(UUID.fromString(value))
    }
}

/**
 * Game identifier.
 */
@JvmInline
value class GameId(override val value: UUID) : EntityId {
    override fun toString() = value.toString()
    companion object {
        fun generate() = GameId(EntityId.generate())
        fun from(value: String) = GameId(UUID.fromString(value))
    }
}

/**
 * Provider identifier.
 */
@JvmInline
value class ProviderId(override val value: UUID) : EntityId {
    override fun toString() = value.toString()
    companion object {
        fun generate() = ProviderId(EntityId.generate())
        fun from(value: String) = ProviderId(UUID.fromString(value))
    }
}

/**
 * Aggregator identifier.
 */
@JvmInline
value class AggregatorId(override val value: UUID) : EntityId {
    override fun toString() = value.toString()
    companion object {
        fun generate() = AggregatorId(EntityId.generate())
        fun from(value: String) = AggregatorId(UUID.fromString(value))
    }
}

/**
 * Collection identifier.
 */
@JvmInline
value class CollectionId(override val value: UUID) : EntityId {
    override fun toString() = value.toString()
    companion object {
        fun generate() = CollectionId(EntityId.generate())
        fun from(value: String) = CollectionId(UUID.fromString(value))
    }
}

/**
 * Player identifier (external, from platform).
 */
@Serializable
@JvmInline
value class PlayerId(val value: String) {
    init {
        require(value.isNotBlank()) { "Player ID cannot be blank" }
    }
}

/**
 * External round identifier (from aggregator).
 */
@Serializable
@JvmInline
value class ExternalRoundId(val value: String) {
    init {
        require(value.isNotBlank()) { "External round ID cannot be blank" }
    }
}

/**
 * Transaction identifier.
 */
@Serializable
@JvmInline
value class TransactionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Transaction ID cannot be blank" }
    }
}

/**
 * FreeSpin identifier.
 */
@Serializable
@JvmInline
value class FreeSpinId(val value: String) {
    init {
        require(value.isNotBlank()) { "FreeSpin ID cannot be blank" }
    }
}

/**
 * Game identity (unique business identifier like "gates-of-olympus").
 */
@Serializable
@JvmInline
value class GameIdentity(val value: String) {
    init {
        require(value.isNotBlank()) { "Game identity cannot be blank" }
    }
}

/**
 * Game symbol (aggregator-specific identifier).
 */
@Serializable
@JvmInline
value class GameSymbol(val value: String) {
    init {
        require(value.isNotBlank()) { "Game symbol cannot be blank" }
    }
}

/**
 * Tag for game categorization.
 */
@Serializable
@JvmInline
value class Tag(val value: String) {
    init {
        require(value.isNotBlank()) { "Tag cannot be blank" }
    }
}
