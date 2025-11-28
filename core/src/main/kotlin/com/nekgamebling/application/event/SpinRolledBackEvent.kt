package com.nekgamebling.application.event

import com.nekgamebling.shared.value.Currency
import kotlinx.serialization.Serializable

/**
 * Event emitted when a spin is rolled back.
 */
@Serializable
data class SpinRolledBackEvent(
    override val gameId: String,
    override val gameIdentity: String,
    override val amount: Int,
    override val currency: Currency,
    override val playerId: String,
    override val freeSpinId: String? = null,
    val reason: String? = null
) : SpinEvent {
    override val routingKey: String = "spin.rolled_back"
}
