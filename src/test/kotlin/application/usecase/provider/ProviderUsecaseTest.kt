package application.usecase.provider

import domain.aggregator.model.AggregatorInfo
import domain.aggregator.repository.AggregatorRepository
import domain.common.error.NotFoundError
import domain.provider.model.Provider
import domain.provider.repository.ProviderRepository
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

class ProviderListUsecaseTest {

    private val providerRepository: ProviderRepository = mockk()
    private val aggregatorRepository: AggregatorRepository = mockk()
    private val usecase = ProviderListUsecase(providerRepository, aggregatorRepository)

    @Test
    fun `invoke returns paginated providers with aggregator info`() = runTest {
        val pageable = Pageable(page = 1, size = 10)
        val aggregatorId = UUID.randomUUID()

        val providers = listOf(
            Provider(
                id = UUID.randomUUID(),
                identity = "provider-1",
                name = "Provider 1",
                aggregatorId = aggregatorId,
                active = true
            ),
            Provider(
                id = UUID.randomUUID(),
                identity = "provider-2",
                name = "Provider 2",
                aggregatorId = aggregatorId,
                active = true
            )
        )
        val page = Page(
            items = providers,
            totalPages = 1,
            totalItems = 2,
            currentPage = 1
        )

        val aggregatorInfo = AggregatorInfo(
            id = aggregatorId,
            identity = "test-aggregator",
            config = emptyMap(),
            aggregator = Aggregator.ONEGAMEHUB,
            active = true
        )

        coEvery { providerRepository.findAll(pageable) } returns page
        coEvery { aggregatorRepository.findById(aggregatorId) } returns aggregatorInfo

        val result = usecase(pageable)

        assertEquals(2, result.items.size)
        assertEquals("provider-1", result.items[0].provider.identity)
        assertEquals(aggregatorInfo, result.items[0].aggregatorInfo)
    }

    @Test
    fun `invoke filters out providers without aggregator`() = runTest {
        val pageable = Pageable(page = 1, size = 10)
        val aggregatorId = UUID.randomUUID()

        val providers = listOf(
            Provider(
                id = UUID.randomUUID(),
                identity = "provider-1",
                name = "Provider 1",
                aggregatorId = aggregatorId,
                active = true
            ),
            Provider(
                id = UUID.randomUUID(),
                identity = "provider-2",
                name = "Provider 2",
                aggregatorId = null,
                active = true
            )
        )
        val page = Page(
            items = providers,
            totalPages = 1,
            totalItems = 2,
            currentPage = 1
        )

        val aggregatorInfo = AggregatorInfo(
            id = aggregatorId,
            identity = "test-aggregator",
            config = emptyMap(),
            aggregator = Aggregator.ONEGAMEHUB,
            active = true
        )

        coEvery { providerRepository.findAll(pageable) } returns page
        coEvery { aggregatorRepository.findById(aggregatorId) } returns aggregatorInfo

        val result = usecase(pageable)

        assertEquals(1, result.items.size)
        assertEquals("provider-1", result.items[0].provider.identity)
    }

    @Test
    fun `invoke applies active filter`() = runTest {
        val pageable = Pageable(page = 1, size = 10)
        val aggregatorId = UUID.randomUUID()

        val providers = listOf(
            Provider(
                id = UUID.randomUUID(),
                identity = "active-provider",
                name = "Active Provider",
                aggregatorId = aggregatorId,
                active = true
            ),
            Provider(
                id = UUID.randomUUID(),
                identity = "inactive-provider",
                name = "Inactive Provider",
                aggregatorId = aggregatorId,
                active = false
            )
        )
        val page = Page(
            items = providers,
            totalPages = 1,
            totalItems = 2,
            currentPage = 1
        )

        val aggregatorInfo = AggregatorInfo(
            id = aggregatorId,
            identity = "test-aggregator",
            config = emptyMap(),
            aggregator = Aggregator.ONEGAMEHUB,
            active = true
        )

        coEvery { providerRepository.findAll(pageable) } returns page
        coEvery { aggregatorRepository.findById(aggregatorId) } returns aggregatorInfo

        val result = usecase(pageable) { it.withActive(true) }

        assertEquals(1, result.items.size)
        assertEquals("active-provider", result.items[0].provider.identity)
    }

    @Test
    fun `invoke applies query filter`() = runTest {
        val pageable = Pageable(page = 1, size = 10)
        val aggregatorId = UUID.randomUUID()

        val providers = listOf(
            Provider(
                id = UUID.randomUUID(),
                identity = "matching-provider",
                name = "Matching Provider",
                aggregatorId = aggregatorId,
                active = true
            ),
            Provider(
                id = UUID.randomUUID(),
                identity = "other-provider",
                name = "Other Provider",
                aggregatorId = aggregatorId,
                active = true
            )
        )
        val page = Page(
            items = providers,
            totalPages = 1,
            totalItems = 2,
            currentPage = 1
        )

        val aggregatorInfo = AggregatorInfo(
            id = aggregatorId,
            identity = "test-aggregator",
            config = emptyMap(),
            aggregator = Aggregator.ONEGAMEHUB,
            active = true
        )

        coEvery { providerRepository.findAll(pageable) } returns page
        coEvery { aggregatorRepository.findById(aggregatorId) } returns aggregatorInfo

        val result = usecase(pageable) { it.withQuery("Matching") }

        assertEquals(1, result.items.size)
        assertEquals("matching-provider", result.items[0].provider.identity)
    }
}

class UpdateProviderUsecaseTest {

    private val providerRepository: ProviderRepository = mockk()
    private val usecase = UpdateProviderUsecase(providerRepository)

    @Test
    fun `invoke updates provider successfully`() = runTest {
        val providerId = UUID.randomUUID()
        val existingProvider = Provider(
            id = providerId,
            identity = "test-provider",
            name = "Test Provider",
            order = 100,
            active = true
        )

        val providerSlot = slot<Provider>()

        coEvery { providerRepository.findByIdentity("test-provider") } returns existingProvider
        coEvery { providerRepository.update(capture(providerSlot)) } answers { providerSlot.captured }

        val result = usecase(
            identity = "test-provider",
            order = 50,
            active = false
        )

        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(50, updated.order)
        assertEquals(false, updated.active)
        coVerify(exactly = 1) { providerRepository.update(any()) }
    }

    @Test
    fun `invoke returns failure when provider not found`() = runTest {
        coEvery { providerRepository.findByIdentity("non-existent") } returns null

        val result = usecase(identity = "non-existent", active = false)

        assertTrue(result.isFailure)
        assertIs<NotFoundError>(result.exceptionOrNull())
    }

    @Test
    fun `invoke preserves unchanged fields`() = runTest {
        val existingProvider = Provider(
            id = UUID.randomUUID(),
            identity = "test-provider",
            name = "Test Provider",
            order = 100,
            active = true
        )

        val providerSlot = slot<Provider>()

        coEvery { providerRepository.findByIdentity("test-provider") } returns existingProvider
        coEvery { providerRepository.update(capture(providerSlot)) } answers { providerSlot.captured }

        val result = usecase(identity = "test-provider", order = 25)

        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(25, updated.order)
        assertEquals(true, updated.active)
    }
}

class AssignProviderToAggregatorUsecaseTest {

    private val providerRepository: ProviderRepository = mockk()
    private val aggregatorRepository: AggregatorRepository = mockk()
    private val usecase = AssignProviderToAggregatorUsecase(providerRepository, aggregatorRepository)

    @Test
    fun `invoke assigns provider to aggregator successfully`() = runTest {
        val providerId = UUID.randomUUID()
        val aggregatorId = UUID.randomUUID()

        val provider = Provider(
            id = providerId,
            identity = "test-provider",
            name = "Test Provider"
        )
        val aggregator = AggregatorInfo(
            id = aggregatorId,
            identity = "test-aggregator",
            config = emptyMap(),
            aggregator = Aggregator.ONEGAMEHUB
        )

        coEvery { providerRepository.findById(providerId) } returns provider
        coEvery { aggregatorRepository.findById(aggregatorId) } returns aggregator
        coEvery { providerRepository.assignToAggregator(providerId, aggregatorId) } returns true

        val result = usecase(providerId, aggregatorId)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { providerRepository.assignToAggregator(providerId, aggregatorId) }
    }

    @Test
    fun `invoke returns failure when provider not found`() = runTest {
        val providerId = UUID.randomUUID()
        val aggregatorId = UUID.randomUUID()

        coEvery { providerRepository.findById(providerId) } returns null

        val result = usecase(providerId, aggregatorId)

        assertTrue(result.isFailure)
        assertIs<NotFoundError>(result.exceptionOrNull())
        coVerify(exactly = 0) { providerRepository.assignToAggregator(any(), any()) }
    }

    @Test
    fun `invoke returns failure when aggregator not found`() = runTest {
        val providerId = UUID.randomUUID()
        val aggregatorId = UUID.randomUUID()

        val provider = Provider(
            id = providerId,
            identity = "test-provider",
            name = "Test Provider"
        )

        coEvery { providerRepository.findById(providerId) } returns provider
        coEvery { aggregatorRepository.findById(aggregatorId) } returns null

        val result = usecase(providerId, aggregatorId)

        assertTrue(result.isFailure)
        assertIs<NotFoundError>(result.exceptionOrNull())
        coVerify(exactly = 0) { providerRepository.assignToAggregator(any(), any()) }
    }
}
