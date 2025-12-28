package infrastructure.messaging

import domain.common.event.IntegrationEvent
import application.port.outbound.EventPublisherAdapter
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.*
import org.slf4j.LoggerFactory

/**
 * RabbitMQ implementation of EventPublisherAdapter.
 * Publishes domain events to the configured exchange with routing keys.
 */
class RabbitMqEventPublisher(
    private val application: Application,
    private val exchangeName: String
) : EventPublisherAdapter {

    private val logger = LoggerFactory.getLogger(RabbitMqEventPublisher::class.java)

    override suspend fun publish(event: IntegrationEvent) {
        logger.info("Publishing event [${event.routingKey}]: $event")

        application.rabbitmq {
            basicPublish {
                exchange = exchangeName
                routingKey = event.routingKey
                message(event)
            }
        }
    }
}
