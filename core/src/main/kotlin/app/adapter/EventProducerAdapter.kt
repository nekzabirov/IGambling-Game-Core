package app.adapter

import app.event.IEvent

interface EventProducerAdapter {
    suspend fun publish(event: IEvent)
}