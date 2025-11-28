package com.nekgamebling.application.event

import com.nekgamebling.shared.value.Currency
import kotlinx.serialization.Serializable

/**
 * Base interface for spin-related events.
 */
@Serializable
sealed interface SpinEvent : DomainEvent {
    val gameId: String
    val gameIdentity: String
    val amount: Int
    val currency: Currency
    val playerId: String
    val freeSpinId: String?
}
