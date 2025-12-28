package application.usecase.game

import domain.common.event.GameFavouriteAddedEvent
import domain.common.event.GameFavouriteRemovedEvent
import application.port.outbound.EventPublisherAdapter
import domain.common.error.NotFoundError
import domain.game.model.Game
import domain.game.repository.GameFavouriteRepository
import domain.game.repository.GameRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AddGameFavouriteUsecaseTest {

    private val gameRepository: GameRepository = mockk()
    private val favouriteRepository: GameFavouriteRepository = mockk()
    private val eventPublisher: EventPublisherAdapter = mockk(relaxed = true)
    private val usecase = AddGameFavouriteUsecase(gameRepository, favouriteRepository, eventPublisher)

    @Test
    fun `invoke adds favourite successfully and publishes event`() = runTest {
        val gameId = UUID.randomUUID()
        val game = Game(
            id = gameId,
            identity = "test-game",
            name = "Test Game",
            providerId = UUID.randomUUID()
        )
        val playerId = "player-123"

        val eventSlot = slot<GameFavouriteAddedEvent>()

        coEvery { gameRepository.findByIdentity("test-game") } returns game
        coEvery { favouriteRepository.add(playerId, gameId) } returns true
        coEvery { eventPublisher.publish(capture(eventSlot)) } returns Unit

        val result = usecase("test-game", playerId)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { favouriteRepository.add(playerId, gameId) }
        coVerify(exactly = 1) { eventPublisher.publish(any<GameFavouriteAddedEvent>()) }

        val publishedEvent = eventSlot.captured
        assertEquals(gameId.toString(), publishedEvent.gameId)
        assertEquals("test-game", publishedEvent.gameIdentity)
        assertEquals(playerId, publishedEvent.playerId)
    }

    @Test
    fun `invoke returns failure when game not found`() = runTest {
        coEvery { gameRepository.findByIdentity("non-existent") } returns null

        val result = usecase("non-existent", "player-123")

        assertTrue(result.isFailure)
        assertIs<NotFoundError>(result.exceptionOrNull())
        coVerify(exactly = 0) { favouriteRepository.add(any(), any()) }
        coVerify(exactly = 0) { eventPublisher.publish(any()) }
    }
}

class RemoveGameFavouriteUsecaseTest {

    private val gameRepository: GameRepository = mockk()
    private val favouriteRepository: GameFavouriteRepository = mockk()
    private val eventPublisher: EventPublisherAdapter = mockk(relaxed = true)
    private val usecase = RemoveGameFavouriteUsecase(gameRepository, favouriteRepository, eventPublisher)

    @Test
    fun `invoke removes favourite successfully and publishes event`() = runTest {
        val gameId = UUID.randomUUID()
        val game = Game(
            id = gameId,
            identity = "test-game",
            name = "Test Game",
            providerId = UUID.randomUUID()
        )
        val playerId = "player-123"

        val eventSlot = slot<GameFavouriteRemovedEvent>()

        coEvery { gameRepository.findByIdentity("test-game") } returns game
        coEvery { favouriteRepository.remove(playerId, gameId) } returns true
        coEvery { eventPublisher.publish(capture(eventSlot)) } returns Unit

        val result = usecase("test-game", playerId)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { favouriteRepository.remove(playerId, gameId) }
        coVerify(exactly = 1) { eventPublisher.publish(any<GameFavouriteRemovedEvent>()) }

        val publishedEvent = eventSlot.captured
        assertEquals(gameId.toString(), publishedEvent.gameId)
        assertEquals("test-game", publishedEvent.gameIdentity)
        assertEquals(playerId, publishedEvent.playerId)
    }

    @Test
    fun `invoke returns failure when game not found`() = runTest {
        coEvery { gameRepository.findByIdentity("non-existent") } returns null

        val result = usecase("non-existent", "player-123")

        assertTrue(result.isFailure)
        assertIs<NotFoundError>(result.exceptionOrNull())
        coVerify(exactly = 0) { favouriteRepository.remove(any(), any()) }
        coVerify(exactly = 0) { eventPublisher.publish(any()) }
    }
}
