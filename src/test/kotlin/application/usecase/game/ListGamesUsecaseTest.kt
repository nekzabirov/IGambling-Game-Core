package application.usecase.game

import domain.game.model.Game
import domain.game.model.GameVariant
import domain.game.repository.GameFilter
import domain.game.repository.GameListItem
import domain.game.repository.GameRepository
import domain.provider.model.Provider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import domain.common.value.Aggregator
import shared.value.Page
import shared.value.Pageable
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ListGamesUsecaseTest {

    private val gameRepository: GameRepository = mockk()
    private val usecase = ListGamesUsecase(gameRepository)

    @Test
    fun `invoke returns paginated games from repository`() = runTest {
        val pageable = Pageable(page = 1, size = 10)
        val filter = GameFilter.EMPTY
        val expectedItems = listOf(
            createGameListItem("game-1"),
            createGameListItem("game-2")
        )
        val expectedPage = Page(
            items = expectedItems,
            totalPages = 1,
            totalItems = 2,
            currentPage = 1
        )

        coEvery { gameRepository.findAll(pageable, filter) } returns expectedPage

        val result = usecase(pageable, filter)

        assertEquals(expectedPage, result)
        coVerify(exactly = 1) { gameRepository.findAll(pageable, filter) }
    }

    @Test
    fun `invoke with filter builder applies filter correctly`() = runTest {
        val pageable = Pageable(page = 1, size = 20)
        val expectedPage = Page<GameListItem>(
            items = emptyList(),
            totalPages = 0,
            totalItems = 0,
            currentPage = 1
        )

        coEvery { gameRepository.findAll(pageable, any()) } returns expectedPage

        val result = usecase(pageable) {}

        assertEquals(expectedPage, result)
        coVerify(exactly = 1) { gameRepository.findAll(pageable, any()) }
    }

    @Test
    fun `invoke returns empty page when no games found`() = runTest {
        val pageable = Pageable(page = 1, size = 10)
        val emptyPage = Page<GameListItem>(
            items = emptyList(),
            totalPages = 0,
            totalItems = 0,
            currentPage = 1
        )

        coEvery { gameRepository.findAll(pageable, any()) } returns emptyPage

        val result = usecase(pageable)

        assertEquals(0, result.items.size)
        assertEquals(0, result.totalItems)
    }

    private fun createGameListItem(identity: String): GameListItem {
        val gameId = UUID.randomUUID()
        val providerId = UUID.randomUUID()
        return GameListItem(
            game = Game(
                id = gameId,
                identity = identity,
                name = "Game $identity",
                providerId = providerId
            ),
            variant = GameVariant(
                gameId = gameId,
                symbol = "symbol-$identity",
                name = "Variant $identity",
                providerName = "Provider",
                aggregator = Aggregator.ONEGAMEHUB,
                freeSpinEnable = true,
                freeChipEnable = false,
                jackpotEnable = false,
                demoEnable = true,
                bonusBuyEnable = false,
                locales = emptyList(),
                platforms = emptyList()
            ),
            provider = Provider(
                id = providerId,
                identity = "provider-1",
                name = "Provider"
            )
        )
    }
}
