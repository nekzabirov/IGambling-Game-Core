package application.usecase.aggregator

import domain.aggregator.model.AggregatorInfo
import domain.aggregator.repository.AggregatorRepository
import domain.common.error.DuplicateEntityError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import domain.common.value.Aggregator
import shared.value.Page
import shared.value.Pageable
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AddAggregatorUsecaseTest {

    private val aggregatorRepository: AggregatorRepository = mockk()
    private val usecase = AddAggregatorUsecase(aggregatorRepository)

    @Test
    fun `invoke creates aggregator successfully`() = runTest {
        val identity = "new-aggregator"
        val aggregator = Aggregator.ONEGAMEHUB
        val config = mapOf("apiKey" to "secret", "baseUrl" to "https://api.example.com")

        val aggregatorSlot = slot<AggregatorInfo>()

        coEvery { aggregatorRepository.existsByIdentity(identity) } returns false
        coEvery { aggregatorRepository.save(capture(aggregatorSlot)) } answers { aggregatorSlot.captured }

        val result = usecase(identity, aggregator, config)

        assertTrue(result.isSuccess)
        val created = result.getOrThrow()
        assertEquals(identity, created.identity)
        assertEquals(aggregator, created.aggregator)
        assertEquals(config, created.config)
        assertEquals(true, created.active)
        coVerify(exactly = 1) { aggregatorRepository.save(any()) }
    }

    @Test
    fun `invoke returns failure when aggregator already exists`() = runTest {
        val identity = "existing-aggregator"

        coEvery { aggregatorRepository.existsByIdentity(identity) } returns true

        val result = usecase(identity, Aggregator.ONEGAMEHUB, emptyMap())

        assertTrue(result.isFailure)
        assertIs<DuplicateEntityError>(result.exceptionOrNull())
        coVerify(exactly = 0) { aggregatorRepository.save(any()) }
    }

    @Test
    fun `invoke creates aggregator with empty config`() = runTest {
        val identity = "minimal-aggregator"

        val aggregatorSlot = slot<AggregatorInfo>()

        coEvery { aggregatorRepository.existsByIdentity(identity) } returns false
        coEvery { aggregatorRepository.save(capture(aggregatorSlot)) } answers { aggregatorSlot.captured }

        val result = usecase(identity, Aggregator.ONEGAMEHUB, emptyMap())

        assertTrue(result.isSuccess)
        val created = result.getOrThrow()
        assertEquals(emptyMap(), created.config)
    }
}

class ListAggregatorUsecaseTest {

    private val aggregatorRepository: AggregatorRepository = mockk()
    private val usecase = ListAggregatorUsecase(aggregatorRepository)

    @Test
    fun `invoke returns paginated aggregators`() = runTest {
        val pageable = Pageable(page = 1, size = 10)
        val aggregators = listOf(
            AggregatorInfo(
                id = UUID.randomUUID(),
                identity = "aggregator-1",
                config = emptyMap(),
                aggregator = Aggregator.ONEGAMEHUB,
                active = true
            ),
            AggregatorInfo(
                id = UUID.randomUUID(),
                identity = "aggregator-2",
                config = emptyMap(),
                aggregator = Aggregator.ONEGAMEHUB,
                active = false
            )
        )
        val page = Page(
            items = aggregators,
            totalPages = 1,
            totalItems = 2,
            currentPage = 1
        )

        coEvery { aggregatorRepository.findAll(pageable) } returns page

        val result = usecase(pageable)

        assertEquals(2, result.items.size)
        assertEquals("aggregator-1", result.items[0].identity)
        assertEquals("aggregator-2", result.items[1].identity)
    }

    @Test
    fun `invoke applies active filter`() = runTest {
        val pageable = Pageable(page = 1, size = 10)
        val aggregators = listOf(
            AggregatorInfo(
                id = UUID.randomUUID(),
                identity = "active-aggregator",
                config = emptyMap(),
                aggregator = Aggregator.ONEGAMEHUB,
                active = true
            ),
            AggregatorInfo(
                id = UUID.randomUUID(),
                identity = "inactive-aggregator",
                config = emptyMap(),
                aggregator = Aggregator.ONEGAMEHUB,
                active = false
            )
        )
        val page = Page(
            items = aggregators,
            totalPages = 1,
            totalItems = 2,
            currentPage = 1
        )

        coEvery { aggregatorRepository.findAll(pageable) } returns page

        val result = usecase(pageable) { it.withActive(true) }

        assertEquals(1, result.items.size)
        assertEquals("active-aggregator", result.items[0].identity)
    }

    @Test
    fun `invoke applies query filter`() = runTest {
        val pageable = Pageable(page = 1, size = 10)
        val aggregators = listOf(
            AggregatorInfo(
                id = UUID.randomUUID(),
                identity = "matching-aggregator",
                config = emptyMap(),
                aggregator = Aggregator.ONEGAMEHUB,
                active = true
            ),
            AggregatorInfo(
                id = UUID.randomUUID(),
                identity = "other-aggregator",
                config = emptyMap(),
                aggregator = Aggregator.ONEGAMEHUB,
                active = true
            )
        )
        val page = Page(
            items = aggregators,
            totalPages = 1,
            totalItems = 2,
            currentPage = 1
        )

        coEvery { aggregatorRepository.findAll(pageable) } returns page

        val result = usecase(pageable) { it.withQuery("matching") }

        assertEquals(1, result.items.size)
        assertEquals("matching-aggregator", result.items[0].identity)
    }

    @Test
    fun `invoke applies type filter`() = runTest {
        val pageable = Pageable(page = 1, size = 10)
        val aggregators = listOf(
            AggregatorInfo(
                id = UUID.randomUUID(),
                identity = "onegamehub-aggregator",
                config = emptyMap(),
                aggregator = Aggregator.ONEGAMEHUB,
                active = true
            )
        )
        val page = Page(
            items = aggregators,
            totalPages = 1,
            totalItems = 1,
            currentPage = 1
        )

        coEvery { aggregatorRepository.findAll(pageable) } returns page

        val result = usecase(pageable) { it.withType(Aggregator.ONEGAMEHUB) }

        assertEquals(1, result.items.size)
        assertEquals(Aggregator.ONEGAMEHUB, result.items[0].aggregator)
    }

    @Test
    fun `invoke returns empty page when no aggregators match`() = runTest {
        val pageable = Pageable(page = 1, size = 10)
        val emptyPage = Page<AggregatorInfo>(
            items = emptyList(),
            totalPages = 0,
            totalItems = 0,
            currentPage = 1
        )

        coEvery { aggregatorRepository.findAll(pageable) } returns emptyPage

        val result = usecase(pageable)

        assertEquals(0, result.items.size)
    }
}

class ListAllActiveAggregatorUsecaseTest {

    private val aggregatorRepository: AggregatorRepository = mockk()
    private val usecase = ListAllActiveAggregatorUsecase(aggregatorRepository)

    @Test
    fun `invoke returns all active aggregators`() = runTest {
        val activeAggregators = listOf(
            AggregatorInfo(
                id = UUID.randomUUID(),
                identity = "active-1",
                config = emptyMap(),
                aggregator = Aggregator.ONEGAMEHUB,
                active = true
            ),
            AggregatorInfo(
                id = UUID.randomUUID(),
                identity = "active-2",
                config = emptyMap(),
                aggregator = Aggregator.ONEGAMEHUB,
                active = true
            )
        )

        coEvery { aggregatorRepository.findAllActive() } returns activeAggregators

        val result = usecase()

        assertEquals(2, result.size)
        assertEquals("active-1", result[0].identity)
        assertEquals("active-2", result[1].identity)
        coVerify(exactly = 1) { aggregatorRepository.findAllActive() }
    }

    @Test
    fun `invoke returns empty list when no active aggregators`() = runTest {
        coEvery { aggregatorRepository.findAllActive() } returns emptyList()

        val result = usecase()

        assertEquals(0, result.size)
    }
}
