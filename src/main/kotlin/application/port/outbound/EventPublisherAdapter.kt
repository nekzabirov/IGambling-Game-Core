package application.port.outbound

import application.event.DomainEvent

/**
 * Port interface for publishing domain events.
 */
interface EventPublisherAdapter {
    /**
     * Publish a domain event.
     */
    suspend fun publish(event: DomainEvent)

    /**
     * Publish multiple domain events.
     */
    suspend fun publishAll(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}
