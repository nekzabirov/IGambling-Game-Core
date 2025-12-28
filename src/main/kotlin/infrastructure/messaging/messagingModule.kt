package infrastructure.messaging

import application.port.outbound.EventPublisherAdapter
import infrastructure.messaging.consumer.consumeSpinSettled
import io.github.damir.denis.tudor.ktor.server.rabbitmq.RabbitMQ
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.dsl.module

/**
 * Koin module for messaging infrastructure.
 * Configures RabbitMQ connection, consumers, and event publisher.
 */
fun messagingModule(application: Application) = module {
    val rabbitmqUrl = System.getenv("RABBITMQ_URL")
    val exchangeName = System.getenv("RABBITMQ_EXCHANGE")

    application.install(RabbitMQ) {
        uri = rabbitmqUrl
    }

    application.consumeSpinSettled(exchangeName)

    single<EventPublisherAdapter> { RabbitMqEventPublisher(application, exchangeName) }
}