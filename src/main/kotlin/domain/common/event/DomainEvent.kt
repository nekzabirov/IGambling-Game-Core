package domain.common.event

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Base marker interface for all domain events.
 */
interface DomainEvent

/**
 * Integration events are published to message queues (RabbitMQ).
 * These have routing keys for message routing.
 */
@Serializable
sealed interface IntegrationEvent : DomainEvent {
    /**
     * Routing key for message queue.
     */
    val routingKey: String

    /**
     * Timestamp when the event occurred.
     */
    val timestamp: Long
        get() = System.currentTimeMillis()
}

/**
 * Internal domain events are produced by aggregates.
 * These have full event metadata for event sourcing.
 */
interface InternalDomainEvent : DomainEvent {
    val eventId: UUID
    val occurredAt: Instant
    val aggregateId: String
    val aggregateType: String
}

/**
 * Marker interface for session-related integration events.
 */
@Serializable
sealed interface SessionIntegrationEvent : IntegrationEvent

/**
 * Marker interface for game-related integration events.
 */
@Serializable
sealed interface GameIntegrationEvent : IntegrationEvent

/**
 * Marker interface for spin-related integration events.
 */
@Serializable
sealed interface SpinIntegrationEvent : IntegrationEvent

/**
 * Marker interface for session-related internal domain events.
 */
interface SessionDomainEvent : InternalDomainEvent {
    override val aggregateType: String get() = "Session"
}

/**
 * Marker interface for game-related internal domain events.
 */
interface GameDomainEvent : InternalDomainEvent {
    override val aggregateType: String get() = "Game"
}

/**
 * Mixin for entities that can produce domain events.
 */
interface DomainEventProducer {
    val domainEvents: List<DomainEvent>
    fun clearEvents()
}

/**
 * Default implementation of domain event collection management.
 */
class DomainEventRegistry {
    private val _events = mutableListOf<DomainEvent>()

    val events: List<DomainEvent> get() = _events.toList()

    fun register(event: DomainEvent) {
        _events.add(event)
    }

    fun registerAll(events: List<DomainEvent>) {
        _events.addAll(events)
    }

    fun clear() {
        _events.clear()
    }
}
