package domain.session.model

import domain.common.event.DomainEvent
import domain.common.event.DomainEventProducer
import domain.common.event.DomainEventRegistry
import domain.common.value.*
import domain.session.event.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import shared.value.Currency
import domain.common.value.Locale
import domain.common.value.Platform
import domain.common.value.SpinType
import java.math.BigInteger
import java.util.UUID

/**
 * Session aggregate root.
 * A gaming session owns all rounds and spins within it.
 *
 * This is the new DDD-compliant version that encapsulates business logic
 * and produces domain events.
 */
class SessionAggregate private constructor(
    val id: SessionId,
    val gameId: GameId,
    val aggregatorId: AggregatorId,
    val playerId: PlayerId,
    val token: String,
    val externalToken: String?,
    val currency: Currency,
    val locale: Locale,
    val platform: Platform,
    private val _rounds: MutableList<RoundEntity>,
    private val eventRegistry: DomainEventRegistry
) : DomainEventProducer {

    val rounds: List<RoundEntity> get() = _rounds.toList()

    override val domainEvents: List<DomainEvent> get() = eventRegistry.events

    override fun clearEvents() = eventRegistry.clear()

    /**
     * Start a new round or get existing one.
     * Rounds are identified by external round ID from aggregator.
     */
    fun startRound(extRoundId: ExternalRoundId): RoundEntity {
        val existing = _rounds.find { it.extId == extRoundId }
        if (existing != null) {
            require(!existing.isFinished) { "Round ${extRoundId.value} is already finished" }
            return existing
        }

        val round = RoundEntity.create(
            sessionId = id,
            gameId = gameId,
            extId = extRoundId
        )
        _rounds.add(round)
        return round
    }

    /**
     * Place a bet within a round.
     * Creates or gets round, then places the bet spin.
     */
    fun placeBet(
        extRoundId: ExternalRoundId,
        transactionId: TransactionId,
        amount: BigInteger,
        realAmount: BigInteger,
        bonusAmount: BigInteger,
        freeSpinId: FreeSpinId? = null
    ): SpinEntity {
        val round = startRound(extRoundId)
        val spin = round.placeBet(transactionId, amount, realAmount, bonusAmount, freeSpinId)

        eventRegistry.register(
            BetPlacedEvent(
                sessionId = id,
                roundId = round.id,
                spinId = spin.id,
                amount = amount,
                realAmount = realAmount,
                bonusAmount = bonusAmount,
                playerId = playerId,
                currency = currency,
                freeSpinId = freeSpinId
            )
        )

        return spin
    }

    /**
     * Settle a round with win amount.
     * Records the win/loss result.
     */
    fun settle(
        extRoundId: ExternalRoundId,
        transactionId: TransactionId,
        winAmount: BigInteger,
        realAmount: BigInteger,
        bonusAmount: BigInteger,
        freeSpinId: FreeSpinId? = null
    ): SpinEntity {
        val round = findRound(extRoundId)
        val (placeSpin, settleSpin) = round.settle(transactionId, winAmount, realAmount, bonusAmount, freeSpinId)

        eventRegistry.register(
            BetSettledEvent(
                sessionId = id,
                roundId = round.id,
                spinId = settleSpin.id,
                betAmount = placeSpin.amount,
                winAmount = winAmount,
                playerId = playerId,
                currency = currency,
                freeSpinId = freeSpinId
            )
        )

        return settleSpin
    }

    /**
     * Rollback a bet.
     * Creates a rollback spin record.
     */
    fun rollback(
        extRoundId: ExternalRoundId,
        transactionId: TransactionId
    ): SpinEntity {
        val round = findRound(extRoundId)
        val (originalSpin, rollbackSpin) = round.rollback(transactionId)

        eventRegistry.register(
            BetRolledBackEvent(
                sessionId = id,
                roundId = round.id,
                spinId = rollbackSpin.id,
                originalSpinId = originalSpin.id,
                playerId = playerId
            )
        )

        return rollbackSpin
    }

    /**
     * Close a round.
     * Marks the round as finished.
     */
    fun closeRound(extRoundId: ExternalRoundId) {
        val round = findRound(extRoundId)
        round.finish()

        eventRegistry.register(
            RoundFinishedEvent(
                sessionId = id,
                roundId = round.id
            )
        )
    }

    private fun findRound(extRoundId: ExternalRoundId): RoundEntity =
        _rounds.find { it.extId == extRoundId }
            ?: throw IllegalArgumentException("Round not found: ${extRoundId.value}")

    /**
     * Convert to legacy Session data class for backward compatibility.
     */
    fun toLegacy(): Session = Session(
        id = id.value,
        gameId = gameId.value,
        aggregatorId = aggregatorId.value,
        playerId = playerId.value,
        token = token,
        externalToken = externalToken,
        currency = currency,
        locale = locale,
        platform = platform
    )

    companion object {
        /**
         * Open a new session.
         */
        fun open(
            gameId: GameId,
            aggregatorId: AggregatorId,
            playerId: PlayerId,
            token: String,
            externalToken: String? = null,
            currency: Currency,
            locale: Locale,
            platform: Platform
        ): SessionAggregate {
            val sessionId = SessionId.generate()
            val eventRegistry = DomainEventRegistry()

            val session = SessionAggregate(
                id = sessionId,
                gameId = gameId,
                aggregatorId = aggregatorId,
                playerId = playerId,
                token = token,
                externalToken = externalToken,
                currency = currency,
                locale = locale,
                platform = platform,
                _rounds = mutableListOf(),
                eventRegistry = eventRegistry
            )

            eventRegistry.register(
                SessionOpenedEvent(
                    sessionId = sessionId,
                    gameId = gameId,
                    playerId = playerId,
                    currency = currency
                )
            )

            return session
        }

        /**
         * Reconstitute from persistence (no events emitted).
         */
        fun reconstitute(
            id: SessionId,
            gameId: GameId,
            aggregatorId: AggregatorId,
            playerId: PlayerId,
            token: String,
            externalToken: String?,
            currency: Currency,
            locale: Locale,
            platform: Platform,
            rounds: List<RoundEntity>
        ): SessionAggregate = SessionAggregate(
            id = id,
            gameId = gameId,
            aggregatorId = aggregatorId,
            playerId = playerId,
            token = token,
            externalToken = externalToken,
            currency = currency,
            locale = locale,
            platform = platform,
            _rounds = rounds.toMutableList(),
            eventRegistry = DomainEventRegistry()
        )

        /**
         * Create from legacy Session (for migration).
         */
        fun fromLegacy(session: Session): SessionAggregate = reconstitute(
            id = SessionId(session.id),
            gameId = GameId(session.gameId),
            aggregatorId = AggregatorId(session.aggregatorId),
            playerId = PlayerId(session.playerId),
            token = session.token,
            externalToken = session.externalToken,
            currency = session.currency,
            locale = session.locale,
            platform = session.platform,
            rounds = emptyList()
        )
    }
}

/**
 * Round entity within Session aggregate.
 */
class RoundEntity private constructor(
    val id: RoundId,
    val sessionId: SessionId,
    val gameId: GameId,
    val extId: ExternalRoundId,
    private var _status: RoundStatus,
    private val _spins: MutableList<SpinEntity>,
    val createdAt: LocalDateTime,
    private var _finishedAt: LocalDateTime?
) {
    val status: RoundStatus get() = _status
    val isFinished: Boolean get() = _status == RoundStatus.FINISHED
    val finishedAt: LocalDateTime? get() = _finishedAt
    val spins: List<SpinEntity> get() = _spins.toList()

    /**
     * Place a bet in this round.
     */
    fun placeBet(
        transactionId: TransactionId,
        amount: BigInteger,
        realAmount: BigInteger,
        bonusAmount: BigInteger,
        freeSpinId: FreeSpinId?
    ): SpinEntity {
        require(_status == RoundStatus.ACTIVE) { "Cannot place bet on finished round" }
        require(_spins.none { it.type == SpinType.PLACE }) { "Bet already placed for this round" }

        val spin = SpinEntity.place(
            roundId = id,
            transactionId = transactionId,
            amount = amount,
            realAmount = realAmount,
            bonusAmount = bonusAmount,
            freeSpinId = freeSpinId
        )
        _spins.add(spin)
        return spin
    }

    /**
     * Settle the round with win amount.
     */
    fun settle(
        transactionId: TransactionId,
        winAmount: BigInteger,
        realAmount: BigInteger,
        bonusAmount: BigInteger,
        freeSpinId: FreeSpinId?
    ): Pair<SpinEntity, SpinEntity> {
        require(_status == RoundStatus.ACTIVE) { "Cannot settle finished round" }

        val placeSpin = _spins.find { it.type == SpinType.PLACE }
            ?: throw IllegalStateException("No place spin found for round")

        val settleSpin = SpinEntity.settle(
            roundId = id,
            transactionId = transactionId,
            amount = winAmount,
            realAmount = realAmount,
            bonusAmount = bonusAmount,
            referenceId = placeSpin.id,
            freeSpinId = freeSpinId
        )
        _spins.add(settleSpin)

        return placeSpin to settleSpin
    }

    /**
     * Rollback a spin in this round.
     */
    fun rollback(transactionId: TransactionId): Pair<SpinEntity, SpinEntity> {
        val originalSpin = _spins.firstOrNull()
            ?: throw IllegalStateException("No spin to rollback")

        val rollbackSpin = SpinEntity.rollback(
            roundId = id,
            transactionId = transactionId,
            referenceId = originalSpin.id,
            freeSpinId = originalSpin.freeSpinId
        )
        _spins.add(rollbackSpin)

        return originalSpin to rollbackSpin
    }

    /**
     * Finish the round.
     */
    fun finish() {
        require(_status == RoundStatus.ACTIVE) { "Round already finished" }
        _status = RoundStatus.FINISHED
        _finishedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    /**
     * Convert to legacy Round data class.
     */
    fun toLegacy(): Round = Round(
        id = id.value,
        sessionId = sessionId.value,
        gameId = gameId.value,
        extId = extId.value,
        finished = isFinished,
        createdAt = createdAt,
        finishedAt = finishedAt
    )

    companion object {
        fun create(
            sessionId: SessionId,
            gameId: GameId,
            extId: ExternalRoundId
        ): RoundEntity = RoundEntity(
            id = RoundId.generate(),
            sessionId = sessionId,
            gameId = gameId,
            extId = extId,
            _status = RoundStatus.ACTIVE,
            _spins = mutableListOf(),
            createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
            _finishedAt = null
        )

        fun reconstitute(
            id: RoundId,
            sessionId: SessionId,
            gameId: GameId,
            extId: ExternalRoundId,
            status: RoundStatus,
            spins: List<SpinEntity>,
            createdAt: LocalDateTime,
            finishedAt: LocalDateTime?
        ): RoundEntity = RoundEntity(
            id = id,
            sessionId = sessionId,
            gameId = gameId,
            extId = extId,
            _status = status,
            _spins = spins.toMutableList(),
            createdAt = createdAt,
            _finishedAt = finishedAt
        )
    }
}

/**
 * Round status.
 */
enum class RoundStatus {
    ACTIVE,
    FINISHED
}

/**
 * Spin entity within Round.
 */
data class SpinEntity(
    val id: SpinId,
    val roundId: RoundId,
    val type: SpinType,
    val amount: BigInteger,
    val realAmount: BigInteger,
    val bonusAmount: BigInteger,
    val transactionId: TransactionId,
    val referenceId: SpinId? = null,
    val freeSpinId: FreeSpinId? = null
) {
    /**
     * Convert to legacy Spin data class.
     */
    fun toLegacy(): Spin = Spin(
        id = id.value,
        roundId = roundId.value,
        type = type,
        amount = amount,
        realAmount = realAmount,
        bonusAmount = bonusAmount,
        extId = transactionId.value,
        referenceId = referenceId?.value,
        freeSpinId = freeSpinId?.value
    )

    companion object {
        fun place(
            roundId: RoundId,
            transactionId: TransactionId,
            amount: BigInteger,
            realAmount: BigInteger,
            bonusAmount: BigInteger,
            freeSpinId: FreeSpinId?
        ): SpinEntity = SpinEntity(
            id = SpinId.generate(),
            roundId = roundId,
            type = SpinType.PLACE,
            amount = amount,
            realAmount = realAmount,
            bonusAmount = bonusAmount,
            transactionId = transactionId,
            freeSpinId = freeSpinId
        )

        fun settle(
            roundId: RoundId,
            transactionId: TransactionId,
            amount: BigInteger,
            realAmount: BigInteger,
            bonusAmount: BigInteger,
            referenceId: SpinId,
            freeSpinId: FreeSpinId?
        ): SpinEntity = SpinEntity(
            id = SpinId.generate(),
            roundId = roundId,
            type = SpinType.SETTLE,
            amount = amount,
            realAmount = realAmount,
            bonusAmount = bonusAmount,
            transactionId = transactionId,
            referenceId = referenceId,
            freeSpinId = freeSpinId
        )

        fun rollback(
            roundId: RoundId,
            transactionId: TransactionId,
            referenceId: SpinId,
            freeSpinId: FreeSpinId?
        ): SpinEntity = SpinEntity(
            id = SpinId.generate(),
            roundId = roundId,
            type = SpinType.ROLLBACK,
            amount = BigInteger.ZERO,
            realAmount = BigInteger.ZERO,
            bonusAmount = BigInteger.ZERO,
            transactionId = transactionId,
            referenceId = referenceId,
            freeSpinId = freeSpinId
        )
    }
}
