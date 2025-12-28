package domain.game.model

import domain.common.event.DomainEvent
import domain.common.event.DomainEventProducer
import domain.common.event.GameDomainEvent
import shared.value.ImageMap
import java.time.Instant
import java.util.UUID

/**
 * Game aggregate root with rich business logic and domain events.
 *
 * This is the DDD-style aggregate that encapsulates game behavior
 * and produces domain events for state changes.
 */
class GameAggregate private constructor(
    val id: UUID,
    val identity: String,
    val name: String,
    val providerId: UUID,
    private var _images: ImageMap,
    private var _bonusBetEnable: Boolean,
    private var _bonusWageringEnable: Boolean,
    private val _tags: MutableSet<String>,
    private var _active: Boolean
) : DomainEventProducer {

    // Read-only accessors
    val images: ImageMap get() = _images
    val bonusBetEnable: Boolean get() = _bonusBetEnable
    val bonusWageringEnable: Boolean get() = _bonusWageringEnable
    val tags: Set<String> get() = _tags.toSet()
    val active: Boolean get() = _active

    // Domain events
    private val _domainEvents = mutableListOf<GameDomainEvent>()
    override val domainEvents: List<DomainEvent> get() = _domainEvents.toList()
    override fun clearEvents() { _domainEvents.clear() }

    // ===============================
    // Business Logic
    // ===============================

    /**
     * Add a tag to the game.
     * @return true if tag was added, false if already present
     */
    fun addTag(tag: String): Boolean {
        require(tag.isNotBlank()) { "Tag cannot be blank" }

        if (_tags.contains(tag)) return false

        _tags.add(tag)
        _domainEvents.add(GameTagAddedEvent(id, tag))
        return true
    }

    /**
     * Remove a tag from the game.
     * @return true if tag was removed, false if not present
     */
    fun removeTag(tag: String): Boolean {
        if (!_tags.contains(tag)) return false

        _tags.remove(tag)
        _domainEvents.add(GameTagRemovedEvent(id, tag))
        return true
    }

    /**
     * Activate the game (make it playable).
     * @return true if status changed, false if already active
     */
    fun activate(): Boolean {
        if (_active) return false

        _active = true
        _domainEvents.add(GameActivatedEvent(id))
        return true
    }

    /**
     * Deactivate the game (make it not playable).
     * @return true if status changed, false if already inactive
     */
    fun deactivate(): Boolean {
        if (!_active) return false

        _active = false
        _domainEvents.add(GameDeactivatedEvent(id))
        return true
    }

    /**
     * Update game images.
     */
    fun updateImages(images: ImageMap) {
        if (_images != images) {
            _images = images
            _domainEvents.add(GameImagesUpdatedEvent(id, images))
        }
    }

    /**
     * Update bonus configuration.
     */
    fun updateBonusConfiguration(bonusBetEnable: Boolean, bonusWageringEnable: Boolean) {
        val changed = _bonusBetEnable != bonusBetEnable || _bonusWageringEnable != bonusWageringEnable
        if (changed) {
            _bonusBetEnable = bonusBetEnable
            _bonusWageringEnable = bonusWageringEnable
            _domainEvents.add(GameBonusConfigUpdatedEvent(id, bonusBetEnable, bonusWageringEnable))
        }
    }

    // ===============================
    // Query Methods
    // ===============================

    fun isPlayable(): Boolean = _active
    fun hasTag(tag: String): Boolean = _tags.contains(tag)

    // ===============================
    // Conversion
    // ===============================

    /**
     * Convert to legacy Game data class.
     */
    fun toLegacy(): Game = Game(
        id = id,
        identity = identity,
        name = name,
        providerId = providerId,
        images = _images,
        bonusBetEnable = _bonusBetEnable,
        bonusWageringEnable = _bonusWageringEnable,
        tags = _tags.toList(),
        active = _active
    )

    companion object {
        /**
         * Create a new game.
         */
        fun create(
            id: UUID = UUID.randomUUID(),
            identity: String,
            name: String,
            providerId: UUID,
            images: ImageMap = ImageMap.EMPTY,
            bonusBetEnable: Boolean = true,
            bonusWageringEnable: Boolean = true,
            tags: Set<String> = emptySet(),
            active: Boolean = true
        ): GameAggregate {
            require(identity.isNotBlank()) { "Game identity cannot be blank" }
            require(name.isNotBlank()) { "Game name cannot be blank" }

            val game = GameAggregate(
                id = id,
                identity = identity,
                name = name,
                providerId = providerId,
                _images = images,
                _bonusBetEnable = bonusBetEnable,
                _bonusWageringEnable = bonusWageringEnable,
                _tags = tags.toMutableSet(),
                _active = active
            )
            game._domainEvents.add(GameCreatedEvent(id, identity, name, providerId))
            return game
        }

        /**
         * Reconstitute from legacy Game data class.
         */
        fun fromLegacy(game: Game): GameAggregate = GameAggregate(
            id = game.id,
            identity = game.identity,
            name = game.name,
            providerId = game.providerId,
            _images = game.images,
            _bonusBetEnable = game.bonusBetEnable,
            _bonusWageringEnable = game.bonusWageringEnable,
            _tags = game.tags.toMutableSet(),
            _active = game.active
        )
    }
}

// ===============================
// Domain Events
// ===============================

data class GameCreatedEvent(
    val gameId: UUID,
    val identity: String,
    val name: String,
    val providerId: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now()
) : GameDomainEvent {
    override val aggregateId: String = gameId.toString()
}

data class GameActivatedEvent(
    val gameId: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now()
) : GameDomainEvent {
    override val aggregateId: String = gameId.toString()
}

data class GameDeactivatedEvent(
    val gameId: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now()
) : GameDomainEvent {
    override val aggregateId: String = gameId.toString()
}

data class GameTagAddedEvent(
    val gameId: UUID,
    val tag: String,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now()
) : GameDomainEvent {
    override val aggregateId: String = gameId.toString()
}

data class GameTagRemovedEvent(
    val gameId: UUID,
    val tag: String,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now()
) : GameDomainEvent {
    override val aggregateId: String = gameId.toString()
}

data class GameImagesUpdatedEvent(
    val gameId: UUID,
    val images: ImageMap,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now()
) : GameDomainEvent {
    override val aggregateId: String = gameId.toString()
}

data class GameBonusConfigUpdatedEvent(
    val gameId: UUID,
    val bonusBetEnable: Boolean,
    val bonusWageringEnable: Boolean,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now()
) : GameDomainEvent {
    override val aggregateId: String = gameId.toString()
}
