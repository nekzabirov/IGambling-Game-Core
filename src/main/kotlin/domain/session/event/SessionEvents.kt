package domain.session.event

import domain.common.event.SessionDomainEvent
import domain.common.value.*
import shared.value.Currency
import java.math.BigInteger
import java.time.Instant
import java.util.UUID

/**
 * Event emitted when a new session is opened.
 */
data class SessionOpenedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now(),
    val sessionId: SessionId,
    val gameId: GameId,
    val playerId: PlayerId,
    val currency: Currency
) : SessionDomainEvent {
    override val aggregateId: String get() = sessionId.value.toString()
}

/**
 * Event emitted when a bet is placed.
 */
data class BetPlacedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now(),
    val sessionId: SessionId,
    val roundId: RoundId,
    val spinId: SpinId,
    val amount: BigInteger,
    val realAmount: BigInteger,
    val bonusAmount: BigInteger,
    val playerId: PlayerId,
    val currency: Currency,
    val freeSpinId: FreeSpinId?
) : SessionDomainEvent {
    override val aggregateId: String get() = sessionId.value.toString()
}

/**
 * Event emitted when a spin is settled (win/loss determined).
 */
data class BetSettledEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now(),
    val sessionId: SessionId,
    val roundId: RoundId,
    val spinId: SpinId,
    val betAmount: BigInteger,
    val winAmount: BigInteger,
    val playerId: PlayerId,
    val currency: Currency,
    val freeSpinId: FreeSpinId?
) : SessionDomainEvent {
    override val aggregateId: String get() = sessionId.value.toString()

    val isWin: Boolean get() = winAmount > BigInteger.ZERO
}

/**
 * Event emitted when a bet is rolled back.
 */
data class BetRolledBackEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now(),
    val sessionId: SessionId,
    val roundId: RoundId,
    val spinId: SpinId,
    val originalSpinId: SpinId,
    val playerId: PlayerId
) : SessionDomainEvent {
    override val aggregateId: String get() = sessionId.value.toString()
}

/**
 * Event emitted when a round is finished.
 */
data class RoundFinishedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now(),
    val sessionId: SessionId,
    val roundId: RoundId
) : SessionDomainEvent {
    override val aggregateId: String get() = sessionId.value.toString()
}
