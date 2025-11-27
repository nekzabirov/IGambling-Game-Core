package infrastructure.adapter

import app.adapter.EventProducerAdapter
import app.event.IEvent
import io.github.damir.denis.tudor.ktor.server.rabbitmq.RabbitMQ
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.*
import sharedPluginConfig

class RabbitMqEventProducer(private val application: Application) : EventProducerAdapter {
    init {
        application.install(RabbitMQ) {
            uri = application.sharedPluginConfig.rabbitMq.url
        }
    }

    override suspend fun publish(event: IEvent) {
        application.rabbitmq {
            basicPublish {
                exchange = application.sharedPluginConfig.rabbitMq.exchange
                routingKey = event.key
                message(event)
            }
        }
    }
}