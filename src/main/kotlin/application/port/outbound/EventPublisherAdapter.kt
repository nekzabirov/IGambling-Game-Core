package application.port.outbound

import domain.common.event.IntegrationEvent

/**
 * Port interface for publishing integration events to message queues.
 */
interface EventPublisherAdapter {
    /**
     * Publish an integration event.
     */
    suspend fun publish(event: IntegrationEvent)

    /**
     * Publish multiple integration events.
     */
    suspend fun publishAll(events: List<IntegrationEvent>) {
        events.forEach { publish(it) }
    }
}
