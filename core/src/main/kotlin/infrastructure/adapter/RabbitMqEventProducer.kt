package infrastructure.adapter

import app.adapter.EventProducerAdapter
import app.event.IEvent
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.*
import sharedPluginConfig
import io.ktor.server.application.log

class RabbitMqEventProducer(private val application: Application) : EventProducerAdapter {
    override suspend fun publish(event: IEvent) {
        application.log.info("Send event ${event.key}: $event")

        application.rabbitmq {
            basicPublish {
                exchange = application.sharedPluginConfig.rabbitMq.exchange
                routingKey = event.key
                message(event)
            }
        }
    }
}