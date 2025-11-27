package app.event

import kotlinx.serialization.Serializable

@Serializable
sealed interface IEvent {
    val key: String
}