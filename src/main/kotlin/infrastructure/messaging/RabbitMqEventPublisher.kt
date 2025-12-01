package infrastructure.messaging

import application.event.DomainEvent
import application.port.outbound.EventPublisherAdapter
import io.ktor.server.application.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * RabbitMQ implementation of EventPublisherAdapter.
 */
class RabbitMqEventPublisher(
    private val application: Application,
    private val exchangeName: String = System.getenv("RABBITMQ_EXCHANGE") ?: "game.event"
) : EventPublisherAdapter {
    private val logger = LoggerFactory.getLogger(RabbitMqEventPublisher::class.java)
    private val json = Json { encodeDefaults = true }

    override suspend fun publish(event: DomainEvent) {
        val message = json.encodeToString(event)

        logger.info("Publishing event [${event.routingKey}]: $message")

        try {
            application.rabbitmq {
                basicPublish(
                    exchangeName,
                    event.routingKey,
                    message.toByteArray()
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to publish event [${event.routingKey}]", e)
            throw e
        }
    }
}

/**
 * Extension function placeholder for RabbitMQ plugin.
 * This should be replaced with actual RabbitMQ plugin integration.
 */
private suspend fun Application.rabbitmq(block: suspend RabbitMqChannel.() -> Unit) {
    // This is a placeholder - actual implementation depends on the RabbitMQ plugin being used
    // The real implementation would get the RabbitMQ channel from the application
    // and execute the block with it
    val channel = RabbitMqChannel()
    block(channel)
}

/**
 * Placeholder for RabbitMQ channel.
 */
class RabbitMqChannel {
    suspend fun basicPublish(exchange: String, routingKey: String, body: ByteArray) {
        // Placeholder - actual implementation would publish to RabbitMQ
    }
}
