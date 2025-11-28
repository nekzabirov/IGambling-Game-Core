package com.nekgamebling.application.event

import com.nekgamebling.shared.value.Currency
import kotlinx.serialization.Serializable

/**
 * Event emitted when a spin is settled (win/loss determined).
 */
@Serializable
data class SpinSettledEvent(
    override val gameId: String,
    override val gameIdentity: String,
    override val amount: Int,
    override val currency: Currency,
    override val playerId: String,
    override val freeSpinId: String? = null,
    val winAmount: Int = 0
) : SpinEvent {
    override val routingKey: String = "spin.settled"
}
