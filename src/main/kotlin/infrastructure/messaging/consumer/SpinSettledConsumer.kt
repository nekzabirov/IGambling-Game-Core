package infrastructure.messaging.consumer

import domain.common.event.SpinSettledEvent
import application.usecase.game.AddGameWinUsecase
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
    val addGameWinUsecase = getKoin().get<AddGameWinUsecase>()

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
                addGameWinUsecase(
                    gameIdentity = event.gameIdentity,
                    playerId = event.playerId,
                    amount = event.amount,
                    currency = event.currency
                )
            }
        }
    }
}