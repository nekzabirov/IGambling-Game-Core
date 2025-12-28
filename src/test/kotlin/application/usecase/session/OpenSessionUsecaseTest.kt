package application.usecase.session

import domain.common.event.SessionOpenedEvent
import application.port.outbound.AggregatorAdapterFactory
import application.port.outbound.AggregatorAdapterRegistry
import application.port.outbound.EventPublisherAdapter
import application.port.outbound.AggregatorLaunchUrlPort
import application.service.GameService
import application.service.SessionService
import domain.aggregator.model.AggregatorInfo
import domain.common.error.AggregatorNotSupportedError
import domain.common.error.NotFoundError
import domain.common.error.ValidationError
import domain.game.model.GameWithDetails
import domain.provider.model.Provider
import domain.session.model.Session
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import domain.common.value.Aggregator
import shared.value.Currency
import shared.value.ImageMap
import domain.common.value.Locale
import domain.common.value.Platform
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OpenSessionUsecaseTest {

    private val gameService: GameService = mockk()
    private val sessionService: SessionService = mockk()
    private val eventPublisher: EventPublisherAdapter = mockk(relaxed = true)
    private val aggregatorRegistry: AggregatorAdapterRegistry = mockk()
    private val usecase = OpenSessionUsecase(gameService, sessionService, eventPublisher, aggregatorRegistry)

    @Test
    fun `invoke opens session successfully`() = runTest {
        val gameId = UUID.randomUUID()
        val aggregatorId = UUID.randomUUID()
        val providerId = UUID.randomUUID()
        val sessionToken = "generated-token"
        val launchUrl = "https://game.example.com/launch"

        val gameWithDetails = createGameWithDetails(
            id = gameId,
            aggregatorId = aggregatorId,
            providerId = providerId,
            locales = listOf(Locale("en")),
            platforms = listOf(Platform.DESKTOP)
        )

        val command = OpenSessionCommand(
            gameIdentity = "test-game",
            playerId = "player-123",
            currency = Currency("EUR"),
            locale = Locale("en"),
            platform = Platform.DESKTOP,
            loggyUrl = "https://lobby.example.com"
        )

        val launchUrlAdapter: AggregatorLaunchUrlPort = mockk()
        val aggregatorFactory: AggregatorAdapterFactory = mockk()

        val sessionSlot = slot<Session>()
        val eventSlot = slot<SessionOpenedEvent>()

        coEvery { gameService.findByIdentity("test-game") } returns Result.success(gameWithDetails)
        every { aggregatorRegistry.getFactory(Aggregator.ONEGAMEHUB) } returns aggregatorFactory
        every { aggregatorFactory.createLaunchUrlAdapter(any()) } returns launchUrlAdapter
        every { sessionService.generateSessionToken() } returns sessionToken
        coEvery { sessionService.createSession(capture(sessionSlot)) } answers {
            Result.success(sessionSlot.captured)
        }
        coEvery { launchUrlAdapter.getLaunchUrl(any(), any(), any(), any(), any(), any(), any(), any()) } returns Result.success(launchUrl)
        coEvery { eventPublisher.publish(capture(eventSlot)) } returns Unit

        val result = usecase(command)

        assertTrue(result.isSuccess)
        val openResult = result.getOrThrow()
        assertEquals(launchUrl, openResult.launchUrl)
        assertEquals(sessionToken, openResult.session.token)
        assertEquals("player-123", openResult.session.playerId)

        coVerify(exactly = 1) { eventPublisher.publish(any<SessionOpenedEvent>()) }
        val event = eventSlot.captured
        assertEquals("player-123", event.playerId)
    }

    @Test
    fun `invoke returns failure when game not found`() = runTest {
        val command = OpenSessionCommand(
            gameIdentity = "non-existent",
            playerId = "player-123",
            currency = Currency("EUR"),
            locale = Locale("en"),
            platform = Platform.DESKTOP,
            loggyUrl = "https://lobby.example.com"
        )

        coEvery { gameService.findByIdentity("non-existent") } returns Result.failure(NotFoundError("Game", "non-existent"))

        val result = usecase(command)

        assertTrue(result.isFailure)
        assertIs<NotFoundError>(result.exceptionOrNull())
        coVerify(exactly = 0) { eventPublisher.publish(any()) }
    }

    @Test
    fun `invoke returns failure when locale not supported`() = runTest {
        val gameWithDetails = createGameWithDetails(
            locales = listOf(Locale("en")),
            platforms = listOf(Platform.DESKTOP)
        )

        val command = OpenSessionCommand(
            gameIdentity = "test-game",
            playerId = "player-123",
            currency = Currency("EUR"),
            locale = Locale("de"),
            platform = Platform.DESKTOP,
            loggyUrl = "https://lobby.example.com"
        )

        coEvery { gameService.findByIdentity("test-game") } returns Result.success(gameWithDetails)

        val result = usecase(command)

        assertTrue(result.isFailure)
        assertIs<ValidationError>(result.exceptionOrNull())
        coVerify(exactly = 0) { eventPublisher.publish(any()) }
    }

    @Test
    fun `invoke returns failure when platform not supported`() = runTest {
        val gameWithDetails = createGameWithDetails(
            locales = listOf(Locale("en")),
            platforms = listOf(Platform.DESKTOP)
        )

        val command = OpenSessionCommand(
            gameIdentity = "test-game",
            playerId = "player-123",
            currency = Currency("EUR"),
            locale = Locale("en"),
            platform = Platform.MOBILE,
            loggyUrl = "https://lobby.example.com"
        )

        coEvery { gameService.findByIdentity("test-game") } returns Result.success(gameWithDetails)

        val result = usecase(command)

        assertTrue(result.isFailure)
        assertIs<ValidationError>(result.exceptionOrNull())
    }

    @Test
    fun `invoke returns failure when aggregator not supported`() = runTest {
        val gameWithDetails = createGameWithDetails(
            locales = listOf(Locale("en")),
            platforms = listOf(Platform.DESKTOP)
        )

        val command = OpenSessionCommand(
            gameIdentity = "test-game",
            playerId = "player-123",
            currency = Currency("EUR"),
            locale = Locale("en"),
            platform = Platform.DESKTOP,
            loggyUrl = "https://lobby.example.com"
        )

        coEvery { gameService.findByIdentity("test-game") } returns Result.success(gameWithDetails)
        every { aggregatorRegistry.getFactory(Aggregator.ONEGAMEHUB) } returns null

        val result = usecase(command)

        assertTrue(result.isFailure)
        assertIs<AggregatorNotSupportedError>(result.exceptionOrNull())
    }

    private fun createGameWithDetails(
        id: UUID = UUID.randomUUID(),
        aggregatorId: UUID = UUID.randomUUID(),
        providerId: UUID = UUID.randomUUID(),
        locales: List<Locale> = emptyList(),
        platforms: List<Platform> = emptyList()
    ) = GameWithDetails(
        id = id,
        identity = "test-game",
        name = "Test Game",
        images = ImageMap.EMPTY,
        symbol = "test-symbol",
        freeSpinEnable = true,
        freeChipEnable = false,
        jackpotEnable = false,
        demoEnable = true,
        bonusBuyEnable = false,
        locales = locales,
        platforms = platforms,
        playLines = 20,
        provider = Provider(
            id = providerId,
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
