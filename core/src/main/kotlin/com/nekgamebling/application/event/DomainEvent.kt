package com.nekgamebling.application.event

import kotlinx.serialization.Serializable

/**
 * Base interface for all domain events.
 */
@Serializable
sealed interface DomainEvent {
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
