package infrastructure.messaging.consumer

import domain.common.event.SpinSettledEvent
import application.port.inbound.command.AddGameWinCommand
import infrastructure.handler.command.AddGameWinCommandHandler
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.*
import io.ktor.server.application.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.getKoin

private const val SPIN_EVENTS_QUEUE = "spin.events"
private const val SPIN_SETTLED_ROUTING_KEY = "spin.settled"

private val json = Json { ignoreUnknownKeys = true }

/**
 * Configures RabbitMQ consumer for SpinSettledEvent messages.
 */
fun Application.consumeSpinSettled(exchangeName: String) = rabbitmq {
    val addGameWinCommandHandler = getKoin().get<AddGameWinCommandHandler>()

    queueBind {
        queue = SPIN_EVENTS_QUEUE
        exchange = exchangeName
        routingKey = SPIN_SETTLED_ROUTING_KEY

        exchangeDeclare {
            exchange = exchangeName
            type = "topic"
            durable = true
        }

        queueDeclare {
            queue = SPIN_EVENTS_QUEUE
            durable = true
        }
    }

    basicConsume {
        queue = SPIN_EVENTS_QUEUE
        autoAck = true

        deliverCallback<String> { message ->
            val event = json.decodeFromString<SpinSettledEvent>(message.body)
            log.info("SpinSettledEvent received: $event")

            // Skip free spins
            if (event.freeSpinId == null) {
                addGameWinCommandHandler.handle(
                    AddGameWinCommand(
                        gameIdentity = event.gameIdentity,
                        playerId = event.playerId,
                        amount = event.amount,
                        currency = event.currency.value
                    )
                )
            }
        }
    }
}
