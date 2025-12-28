package application.usecase.spin

import domain.common.event.SpinPlacedEvent
import domain.common.event.SpinSettledEvent
import application.port.outbound.EventPublisherAdapter
import application.service.GameService
import application.service.SpinService
import application.service.AggregatorService
import domain.aggregator.model.AggregatorInfo
import domain.common.error.NotFoundError
import domain.game.model.Game
import domain.game.model.GameWithDetails
import domain.provider.model.Provider
import domain.session.model.Session
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import domain.common.value.Aggregator
import shared.value.Currency
import shared.value.ImageMap
import domain.common.value.Locale
import domain.common.value.Platform
import java.math.BigInteger
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PlaceSpinUsecaseTest {

    private val gameService: GameService = mockk()
    private val spinService: SpinService = mockk()
    private val aggregatorService: AggregatorService = mockk()
    private val eventPublisher: EventPublisherAdapter = mockk(relaxed = true)
    private val usecase = PlaceSpinUsecase(gameService, spinService, aggregatorService, eventPublisher)

    @Test
    fun `invoke places spin successfully`() = runTest {
        val sessionId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        val aggregatorId = UUID.randomUUID()

        val session = createSession(sessionId, gameId, aggregatorId)
        val game = createGame(gameId)
        val aggregator = createAggregatorInfo(aggregatorId)

        val eventSlot = slot<SpinPlacedEvent>()

        coEvery { aggregatorService.findById(aggregatorId) } returns Result.success(aggregator)
        coEvery { gameService.findBySymbol("game-symbol", Aggregator.ONEGAMEHUB) } returns Result.success(game)
        coEvery { spinService.place(session, game, any()) } returns Result.success(Unit)
        coEvery { eventPublisher.publish(capture(eventSlot)) } returns Unit

        val result = usecase(
            session = session,
            gameSymbol = "game-symbol",
            extRoundId = "round-123",
            transactionId = "tx-123",
            freeSpinId = null,
            amount = 100.toBigInteger()
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { spinService.place(session, game, any()) }
        coVerify(exactly = 1) { eventPublisher.publish(any<SpinPlacedEvent>()) }

        val event = eventSlot.captured
        assertEquals("test-game", event.gameIdentity)
        assertEquals(100.toBigInteger(), event.amount)
        assertEquals("player-123", event.playerId)
    }

    @Test
    fun `invoke returns failure when game not found`() = runTest {
        val sessionId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        val aggregatorId = UUID.randomUUID()

        val session = createSession(sessionId, gameId, aggregatorId)
        val aggregator = createAggregatorInfo(aggregatorId)

        coEvery { aggregatorService.findById(aggregatorId) } returns Result.success(aggregator)
        coEvery { gameService.findBySymbol("unknown-symbol", Aggregator.ONEGAMEHUB) } returns Result.failure(NotFoundError("Game", "unknown-symbol"))

        val result = usecase(
            session = session,
            gameSymbol = "unknown-symbol",
            extRoundId = "round-123",
            transactionId = "tx-123",
            freeSpinId = null,
            amount = 100.toBigInteger()
        )

        assertTrue(result.isFailure)
        assertIs<NotFoundError>(result.exceptionOrNull())
    }

    @Test
    fun `invoke places free spin successfully`() = runTest {
        val sessionId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        val aggregatorId = UUID.randomUUID()
        val freeSpinId = "freespin-123"

        val session = createSession(sessionId, gameId, aggregatorId)
        val game = createGame(gameId)
        val aggregator = createAggregatorInfo(aggregatorId)

        val eventSlot = slot<SpinPlacedEvent>()

        coEvery { aggregatorService.findById(aggregatorId) } returns Result.success(aggregator)
        coEvery { gameService.findBySymbol("game-symbol", Aggregator.ONEGAMEHUB) } returns Result.success(game)
        coEvery { spinService.place(session, game, any()) } returns Result.success(Unit)
        coEvery { eventPublisher.publish(capture(eventSlot)) } returns Unit

        val result = usecase(
            session = session,
            gameSymbol = "game-symbol",
            extRoundId = "round-123",
            transactionId = "tx-123",
            freeSpinId = freeSpinId,
            amount = BigInteger.ZERO
        )

        assertTrue(result.isSuccess)

        val event = eventSlot.captured
        assertEquals(freeSpinId, event.freeSpinId)
    }

    private fun createSession(sessionId: UUID, gameId: UUID, aggregatorId: UUID) = Session(
        id = sessionId,
        gameId = gameId,
        aggregatorId = aggregatorId,
        playerId = "player-123",
        token = "test-token",
        externalToken = null,
        currency = Currency("EUR"),
        locale = Locale("en"),
        platform = Platform.DESKTOP
    )

    private fun createGame(gameId: UUID) = Game(
        id = gameId,
        identity = "test-game",
        name = "Test Game",
        providerId = UUID.randomUUID(),
        active = true
    )

    private fun createAggregatorInfo(aggregatorId: UUID) = AggregatorInfo(
        id = aggregatorId,
        identity = "test-aggregator",
        config = emptyMap(),
        aggregator = Aggregator.ONEGAMEHUB,
        active = true
    )
}

class SettleSpinUsecaseTest {

    private val spinService: SpinService = mockk()
    private val gameService: GameService = mockk()
    private val eventPublisher: EventPublisherAdapter = mockk(relaxed = true)
    private val usecase = SettleSpinUsecase(spinService, gameService, eventPublisher)

    @Test
    fun `invoke settles spin successfully`() = runTest {
        val sessionId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        val aggregatorId = UUID.randomUUID()

        val session = createSession(sessionId, gameId, aggregatorId)
        val gameWithDetails = createGameWithDetails(gameId, aggregatorId)

        val eventSlot = slot<SpinSettledEvent>()

        coEvery { spinService.settle(session, "round-123", any()) } returns Result.success(Unit)
        coEvery { gameService.findById(gameId) } returns Result.success(gameWithDetails)
        coEvery { eventPublisher.publish(capture(eventSlot)) } returns Unit

        val result = usecase(
            session = session,
            extRoundId = "round-123",
            transactionId = "tx-456",
            freeSpinId = null,
            winAmount = 500.toBigInteger()
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { spinService.settle(session, "round-123", any()) }
        coVerify(exactly = 1) { eventPublisher.publish(any<SpinSettledEvent>()) }

        val event = eventSlot.captured
        assertEquals("test-game", event.gameIdentity)
        assertEquals(500.toBigInteger(), event.amount)
        assertEquals("player-123", event.playerId)
    }

    @Test
    fun `invoke returns success for zero win amount`() = runTest {
        val sessionId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        val aggregatorId = UUID.randomUUID()

        val session = createSession(sessionId, gameId, aggregatorId)

        val result = usecase(
            session = session,
            extRoundId = "round-123",
            transactionId = "tx-456",
            freeSpinId = null,
            winAmount = BigInteger.ZERO
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { eventPublisher.publish(any()) }
    }

    @Test
    fun `invoke settles free spin successfully`() = runTest {
        val sessionId = UUID.randomUUID()
        val gameId = UUID.randomUUID()
        val aggregatorId = UUID.randomUUID()
        val freeSpinId = "freespin-123"

        val session = createSession(sessionId, gameId, aggregatorId)
        val gameWithDetails = createGameWithDetails(gameId, aggregatorId)

        val eventSlot = slot<SpinSettledEvent>()

        coEvery { spinService.settle(session, "round-123", any()) } returns Result.success(Unit)
        coEvery { gameService.findById(gameId) } returns Result.success(gameWithDetails)
        coEvery { eventPublisher.publish(capture(eventSlot)) } returns Unit

        val result = usecase(
            session = session,
            extRoundId = "round-123",
            transactionId = "tx-456",
            freeSpinId = freeSpinId,
            winAmount = 1000.toBigInteger()
        )

        assertTrue(result.isSuccess)

        val event = eventSlot.captured
        assertEquals(freeSpinId, event.freeSpinId)
        assertEquals(1000.toBigInteger(), event.amount)
    }

    private fun createSession(sessionId: UUID, gameId: UUID, aggregatorId: UUID) = Session(
        id = sessionId,
        gameId = gameId,
        aggregatorId = aggregatorId,
        playerId = "player-123",
        token = "test-token",
        externalToken = null,
        currency = Currency("EUR"),
        locale = Locale("en"),
        platform = Platform.DESKTOP
    )

    private fun createGameWithDetails(gameId: UUID, aggregatorId: UUID) = GameWithDetails(
        id = gameId,
        identity = "test-game",
        name = "Test Game",
        images = ImageMap.EMPTY,
        symbol = "test-symbol",
        freeSpinEnable = true,
        freeChipEnable = false,
        jackpotEnable = false,
        demoEnable = true,
        bonusBuyEnable = false,
        locales = listOf(Locale("en")),
        platforms = listOf(Platform.DESKTOP),
        playLines = 20,
        provider = Provider(
            id = UUID.randomUUID(),
            identity = "provider-1",
            name = "Test Provider",
            aggregatorId = aggregatorId
        ),
        aggregator = AggregatorInfo(
            id = aggregatorId,
            identity = "test-aggregator",
            config = emptyMap(),
            aggregator = Aggregator.ONEGAMEHUB,
            active = true
        )
    )
}
