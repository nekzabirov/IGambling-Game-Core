package application.usecase.game

import domain.common.event.GameWonEvent
import application.port.outbound.EventPublisherAdapter
import domain.common.error.NotFoundError
import domain.game.model.Game
import domain.game.repository.GameRepository
import domain.game.repository.GameWonRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import shared.value.Currency
import java.math.BigInteger
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AddGameWinUsecaseTest {

    private val gameRepository: GameRepository = mockk()
    private val gameWonRepository: GameWonRepository = mockk()
    private val eventPublisher: EventPublisherAdapter = mockk(relaxed = true)
    private val usecase = AddGameWinUsecase(gameRepository, gameWonRepository, eventPublisher)

    @Test
    fun `invoke records game win and publishes event`() = runTest {
        val gameId = UUID.randomUUID()
        val game = Game(
            id = gameId,
            identity = "test-game",
            name = "Test Game",
            providerId = UUID.randomUUID()
        )
        val playerId = "player-123"
        val amount = 1000.toBigInteger()
        val currency = Currency("EUR")

        val eventSlot = slot<GameWonEvent>()

        coEvery { gameRepository.findByIdentity("test-game") } returns game
        coEvery { gameWonRepository.save(gameId, playerId, amount, "EUR") } returns true
        coEvery { eventPublisher.publish(capture(eventSlot)) } returns Unit

        val result = usecase(
            gameIdentity = "test-game",
            playerId = playerId,
            amount = amount,
            currency = currency
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { gameWonRepository.save(gameId, playerId, amount, "EUR") }
        coVerify(exactly = 1) { eventPublisher.publish(any<GameWonEvent>()) }

        val publishedEvent = eventSlot.captured
        assertEquals(gameId.toString(), publishedEvent.gameId)
        assertEquals("test-game", publishedEvent.gameIdentity)
        assertEquals(playerId, publishedEvent.playerId)
        assertEquals(amount, publishedEvent.amount)
        assertEquals(currency, publishedEvent.currency)
    }

    @Test
    fun `invoke returns failure when game not found`() = runTest {
        coEvery { gameRepository.findByIdentity("non-existent") } returns null

        val result = usecase(
            gameIdentity = "non-existent",
            playerId = "player-123",
            amount = 1000.toBigInteger(),
            currency = Currency("EUR")
        )

        assertTrue(result.isFailure)
        assertIs<NotFoundError>(result.exceptionOrNull())
        coVerify(exactly = 0) { gameWonRepository.save(any(), any(), any(), any()) }
        coVerify(exactly = 0) { eventPublisher.publish(any()) }
    }

    @Test
    fun `invoke records win with zero amount`() = runTest {
        val gameId = UUID.randomUUID()
        val game = Game(
            id = gameId,
            identity = "test-game",
            name = "Test Game",
            providerId = UUID.randomUUID()
        )

        coEvery { gameRepository.findByIdentity("test-game") } returns game
        coEvery { gameWonRepository.save(gameId, any(), BigInteger.ZERO, any()) } returns true
        coEvery { eventPublisher.publish(any()) } returns Unit

        val result = usecase(
            gameIdentity = "test-game",
            playerId = "player-123",
            amount = BigInteger.ZERO,
            currency = Currency("USD")
        )

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { gameWonRepository.save(gameId, "player-123", BigInteger.ZERO, "USD") }
    }
}
