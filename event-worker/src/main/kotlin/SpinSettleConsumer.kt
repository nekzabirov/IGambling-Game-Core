import app.event.SpinEvent
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicConsume
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.exchangeDeclare
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.queueBind
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.queueDeclare
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.Application
import io.ktor.server.application.log

private const val QUEUE_NAME = "spin.events"
private const val ROUTING_KEY = "spin.settled"

fun Application.consumeSpinSettle(exchange: String) = rabbitmq {
    queueBind {
        queue = QUEUE_NAME
        this.exchange = exchange
        routingKey = ROUTING_KEY

        exchangeDeclare {
            this.exchange = exchange
            type = "topic"
            durable = true
        }

        queueDeclare {
            queue = QUEUE_NAME
            durable = true
        }
    }

    basicConsume {
        queue = QUEUE_NAME
        autoAck = true

        deliverCallback<SpinEvent> { msg ->
            val body = msg.body
            log.info("Spin event received: $body")
        }
    }
}