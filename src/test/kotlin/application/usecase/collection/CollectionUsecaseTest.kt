package application.usecase.collection

import domain.collection.model.Collection
import domain.collection.repository.CollectionRepository
import domain.common.error.DuplicateEntityError
import domain.common.error.NotFoundError
import domain.game.model.Game
import domain.game.repository.GameRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import shared.value.LocaleName
import shared.value.Page
import shared.value.Pageable
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AddCollectionUsecaseTest {

    private val collectionRepository: CollectionRepository = mockk()
    private val usecase = AddCollectionUsecase(collectionRepository)

    @Test
    fun `invoke creates collection successfully`() = runTest {
        val identity = "new-collection"
        val name = LocaleName(mapOf("en" to "New Collection"))

        val collectionSlot = slot<Collection>()

        coEvery { collectionRepository.existsByIdentity(identity) } returns false
        coEvery { collectionRepository.save(capture(collectionSlot)) } answers { collectionSlot.captured }

        val result = usecase(identity, name)

        assertTrue(result.isSuccess)
        val collection = result.getOrThrow()
        assertEquals(identity, collection.identity)
        assertEquals(name, collection.name)
        coVerify(exactly = 1) { collectionRepository.save(any()) }
    }

    @Test
    fun `invoke returns failure when collection already exists`() = runTest {
        val identity = "existing-collection"

        coEvery { collectionRepository.existsByIdentity(identity) } returns true

        val result = usecase(identity, LocaleName.EMPTY)

        assertTrue(result.isFailure)
        assertIs<DuplicateEntityError>(result.exceptionOrNull())
        coVerify(exactly = 0) { collectionRepository.save(any()) }
    }
}

class UpdateCollectionUsecaseTest {

    private val collectionRepository: CollectionRepository = mockk()
    private val usecase = UpdateCollectionUsecase(collectionRepository)

    @Test
    fun `invoke updates collection successfully`() = runTest {
        val collectionId = UUID.randomUUID()
        val existingCollection = Collection(
            id = collectionId,
            identity = "test-collection",
            name = LocaleName(mapOf("en" to "Original Name")),
            active = true,
            order = 100
        )

        val newName = LocaleName(mapOf("en" to "Updated Name"))
        val collectionSlot = slot<Collection>()

        coEvery { collectionRepository.findByIdentity("test-collection") } returns existingCollection
        coEvery { collectionRepository.update(capture(collectionSlot)) } answers { collectionSlot.captured }

        val result = usecase(
            identity = "test-collection",
            name = newName,
            active = false
        )

        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(newName, updated.name)
        assertEquals(false, updated.active)
        assertEquals(100, updated.order)
    }

    @Test
    fun `invoke returns failure when collection not found`() = runTest {
        coEvery { collectionRepository.findByIdentity("non-existent") } returns null

        val result = usecase(identity = "non-existent", active = false)

        assertTrue(result.isFailure)
        assertIs<NotFoundError>(result.exceptionOrNull())
    }

    @Test
    fun `invoke preserves unchanged fields`() = runTest {
        val existingCollection = Collection(
            id = UUID.randomUUID(),
            identity = "test-collection",
            name = LocaleName(mapOf("en" to "Original")),
            active = true,
            order = 50
        )

        val collectionSlot = slot<Collection>()

        coEvery { collectionRepository.findByIdentity("test-collection") } returns existingCollection
        coEvery { collectionRepository.update(capture(collectionSlot)) } answers { collectionSlot.captured }

        val result = usecase(identity = "test-collection", order = 25)

        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(LocaleName(mapOf("en" to "Original")), updated.name)
        assertEquals(true, updated.active)
        assertEquals(25, updated.order)
    }
}

class AddGameCollectionUsecaseTest {

    private val collectionRepository: CollectionRepository = mockk()
    private val gameRepository: GameRepository = mockk()
    private val usecase = AddGameCollectionUsecase(collectionRepository, gameRepository)

    @Test
    fun `invoke adds game to collection successfully`() = runTest {
        val collectionId = UUID.randomUUID()
        val gameId = UUID.randomUUID()

        val collection = Collection(
            id = collectionId,
            identity = "test-collection",
            name = LocaleName.EMPTY
        )
        val game = Game(
            id = gameId,
            identity = "test-game",
            name = "Test Game",
            providerId = UUID.randomUUID()
        )

        coEvery { collectionRepository.findByIdentity("test-collection") } returns collection
        coEvery { gameRepository.findByIdentity("test-game") } returns game
        coEvery { collectionRepository.addGame(collectionId, gameId) } returns true

        val result = usecase("test-collection", "test-game")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { collectionRepository.addGame(collectionId, gameId) }
    }

    @Test
    fun `invoke returns failure when collection not found`() = runTest {
        coEvery { collectionRepository.findByIdentity("non-existent") } returns null

        val result = usecase("non-existent", "test-game")

        assertTrue(result.isFailure)
        assertIs<NotFoundError>(result.exceptionOrNull())
    }

    @Test
    fun `invoke returns failure when game not found`() = runTest {
        val collection = Collection(
            id = UUID.randomUUID(),
            identity = "test-collection",
            name = LocaleName.EMPTY
        )

        coEvery { collectionRepository.findByIdentity("test-collection") } returns collection
        coEvery { gameRepository.findByIdentity("non-existent") } returns null

        val result = usecase("test-collection", "non-existent")

        assertTrue(result.isFailure)
        assertIs<NotFoundError>(result.exceptionOrNull())
    }
}

class RemoveGameCollectionUsecaseTest {

    private val collectionRepository: CollectionRepository = mockk()
    private val gameRepository: GameRepository = mockk()
    private val usecase = RemoveGameCollectionUsecase(collectionRepository, gameRepository)

    @Test
    fun `invoke removes game from collection successfully`() = runTest {
        val collectionId = UUID.randomUUID()
        val gameId = UUID.randomUUID()

        val collection = Collection(
            id = collectionId,
            identity = "test-collection",
            name = LocaleName.EMPTY
        )
        val game = Game(
            id = gameId,
            identity = "test-game",
            name = "Test Game",
            providerId = UUID.randomUUID()
        )

        coEvery { collectionRepository.findByIdentity("test-collection") } returns collection
        coEvery { gameRepository.findByIdentity("test-game") } returns game
        coEvery { collectionRepository.removeGame(collectionId, gameId) } returns true

        val result = usecase("test-collection", "test-game")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { collectionRepository.removeGame(collectionId, gameId) }
    }

    @Test
    fun `invoke returns failure when collection not found`() = runTest {
        coEvery { collectionRepository.findByIdentity("non-existent") } returns null

        val result = usecase("non-existent", "test-game")

        assertTrue(result.isFailure)
        assertIs<NotFoundError>(result.exceptionOrNull())
    }
}

class ChangeGameOrderUsecaseTest {

    private val collectionRepository: CollectionRepository = mockk()
    private val gameRepository: GameRepository = mockk()
    private val usecase = ChangeGameOrderUsecase(collectionRepository, gameRepository)

    @Test
    fun `invoke changes game order successfully`() = runTest {
        val collectionId = UUID.randomUUID()
        val gameId = UUID.randomUUID()

        val collection = Collection(
            id = collectionId,
            identity = "test-collection",
            name = LocaleName.EMPTY
        )
        val game = Game(
            id = gameId,
            identity = "test-game",
            name = "Test Game",
            providerId = UUID.randomUUID()
        )

        coEvery { collectionRepository.findByIdentity("test-collection") } returns collection
        coEvery { gameRepository.findByIdentity("test-game") } returns game
        coEvery { collectionRepository.updateGameOrder(collectionId, gameId, 5) } returns true

        val result = usecase("test-collection", "test-game", 5)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { collectionRepository.updateGameOrder(collectionId, gameId, 5) }
    }

    @Test
    fun `invoke returns failure when collection not found`() = runTest {
        coEvery { collectionRepository.findByIdentity("non-existent") } returns null

        val result = usecase("non-existent", "test-game", 5)

        assertTrue(result.isFailure)
        assertIs<NotFoundError>(result.exceptionOrNull())
    }

    @Test
    fun `invoke returns failure when game not found`() = runTest {
        val collection = Collection(
            id = UUID.randomUUID(),
            identity = "test-collection",
            name = LocaleName.EMPTY
        )

        coEvery { collectionRepository.findByIdentity("test-collection") } returns collection
        coEvery { gameRepository.findByIdentity("non-existent") } returns null

        val result = usecase("test-collection", "non-existent", 5)

        assertTrue(result.isFailure)
        assertIs<NotFoundError>(result.exceptionOrNull())
    }
}

class ListCollectionUsecaseTest {

    private val collectionRepository: CollectionRepository = mockk()
    private val usecase = ListCollectionUsecase(collectionRepository)

    @Test
    fun `invoke returns paginated collections`() = runTest {
        val pageable = Pageable(page = 1, size = 10)
        val collections = listOf(
            Collection(UUID.randomUUID(), "collection-1", LocaleName(mapOf("en" to "Collection 1"))),
            Collection(UUID.randomUUID(), "collection-2", LocaleName(mapOf("en" to "Collection 2")))
        )
        val page = Page(
            items = collections,
            totalPages = 1,
            totalItems = 2,
            currentPage = 1
        )

        coEvery { collectionRepository.findAll(pageable, false) } returns page

        val result = usecase(pageable)

        assertEquals(2, result.items.size)
        assertEquals("collection-1", result.items[0].category.identity)
        assertEquals("collection-2", result.items[1].category.identity)
    }

    @Test
    fun `invoke returns empty page when no collections found`() = runTest {
        val pageable = Pageable(page = 1, size = 10)
        val emptyPage = Page<Collection>(
            items = emptyList(),
            totalPages = 0,
            totalItems = 0,
            currentPage = 1
        )

        coEvery { collectionRepository.findAll(pageable, false) } returns emptyPage

        val result = usecase(pageable)

        assertEquals(0, result.items.size)
    }
}
