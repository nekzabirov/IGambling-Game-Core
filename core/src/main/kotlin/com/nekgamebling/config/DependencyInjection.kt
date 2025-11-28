package com.nekgamebling.config

import com.nekgamebling.application.port.inbound.GamePort
import com.nekgamebling.application.port.outbound.*
import com.nekgamebling.application.service.GameService
import com.nekgamebling.application.service.SessionService
import com.nekgamebling.application.service.SpinService
import com.nekgamebling.application.usecase.aggregator.*
import com.nekgamebling.application.usecase.collection.*
import com.nekgamebling.application.usecase.game.*
import com.nekgamebling.application.usecase.provider.*
import com.nekgamebling.application.usecase.session.OpenSessionUsecase
import com.nekgamebling.application.usecase.spin.*
import com.nekgamebling.domain.aggregator.repository.AggregatorRepository
import com.nekgamebling.domain.collection.repository.CollectionRepository
import com.nekgamebling.domain.game.repository.*
import com.nekgamebling.domain.provider.repository.ProviderRepository
import com.nekgamebling.domain.session.repository.RoundRepository
import com.nekgamebling.domain.session.repository.SessionRepository
import com.nekgamebling.domain.session.repository.SpinRepository
import com.nekgamebling.infrastructure.adapter.BaseCurrencyAdapter
import com.nekgamebling.infrastructure.adapter.FakePlayerAdapter
import com.nekgamebling.infrastructure.adapter.FakeWalletAdapter
import com.nekgamebling.infrastructure.aggregator.AggregatorAdapterRegistryImpl
import com.nekgamebling.infrastructure.aggregator.onegamehub.OneGameHubAdapterFactory
import com.nekgamebling.infrastructure.persistence.cache.InMemoryCacheAdapter
import com.nekgamebling.infrastructure.messaging.RabbitMqEventPublisher
import com.nekgamebling.infrastructure.persistence.exposed.adapter.ExposedGamePort
import com.nekgamebling.infrastructure.persistence.exposed.repository.*
import io.ktor.server.application.*
import org.koin.dsl.module

/**
 * Koin module for dependency injection.
 * All dependencies use constructor injection.
 */
fun Application.coreModule() = module {
    // ==========================================
    // Infrastructure - Repositories
    // ==========================================
    single<GameRepository> { ExposedGameRepository() }
    single<GameVariantRepository> { ExposedGameVariantRepository() }
    single<GameFavouriteRepository> { ExposedGameFavouriteRepository() }
    single<GameWonRepository> { ExposedGameWonRepository() }
    single<SessionRepository> { ExposedSessionRepository() }
    single<RoundRepository> { ExposedRoundRepository() }
    single<SpinRepository> { ExposedSpinRepository() }
    single<ProviderRepository> { ExposedProviderRepository() }
    single<CollectionRepository> { ExposedCollectionRepository() }
    single<AggregatorRepository> { ExposedAggregatorRepository() }

    // ==========================================
    // Infrastructure - Ports/Adapters
    // ==========================================
    single<CachePort> { InMemoryCacheAdapter() }
    single<WalletPort> { FakeWalletAdapter() }
    single<PlayerPort> { FakePlayerAdapter() }
    single<CurrencyPort> { BaseCurrencyAdapter() }
    single<EventPublisherPort> { RabbitMqEventPublisher(this@coreModule) }
    single<GamePort> { ExposedGamePort() }

    // Aggregator Infrastructure - Registry Pattern
    single<AggregatorAdapterRegistry> {
        AggregatorAdapterRegistryImpl().apply {
            // Register all aggregator factories here
            register(OneGameHubAdapterFactory())
            // Add more factories as needed:
            // register(AnotherAggregatorFactory())
        }
    }

    // ==========================================
    // Application Services
    // ==========================================
    single { GameService(get(), get()) }
    single { SessionService(get()) }
    single { SpinService(get(), get(), get(), get()) }

    // ==========================================
    // Application Use Cases - Game
    // ==========================================
    factory { ListGamesUsecase(get()) }
    factory { UpdateGameUsecase(get()) }
    factory { AddGameTagUsecase(get()) }
    factory { RemoveGameTagUsecase(get()) }
    factory { AddGameFavouriteUsecase(get(), get(), get()) }
    factory { RemoveGameFavouriteUsecase(get(), get(), get()) }
    factory { DemoGameUsecase(get(), get()) }
    factory { AddGameWonUsecase(get(), get(), get()) }

    // ==========================================
    // Application Use Cases - Session
    // ==========================================
    factory { OpenSessionUsecase(get(), get(), get(), get()) }

    // ==========================================
    // Application Use Cases - Spin
    // ==========================================
    factory { PlaceSpinUsecase(get(), get(), get(), get()) }
    factory { SettleSpinUsecase(get(), get(), get()) }
    factory { GetPresetUsecase(get(), get()) }
    factory { CreateFreespinUsecase(get(), get()) }
    factory { CancelFreespinUsecase(get(), get()) }

    // ==========================================
    // Application Use Cases - Collection
    // ==========================================
    factory { AddCollectionUsecase(get()) }
    factory { UpdateCollectionUsecase(get()) }
    factory { AddGameCollectionUsecase(get(), get()) }
    factory { RemoveGameCollectionUsecase(get(), get()) }
    factory { ChangeGameOrderUsecase(get(), get()) }
    factory { ListCollectionUsecase(get()) }

    // ==========================================
    // Application Use Cases - Provider
    // ==========================================
    factory { ProviderListUsecase(get(), get()) }
    factory { UpdateProviderUsecase(get()) }
    factory { AssignProviderToAggregatorUsecase(get(), get()) }

    // ==========================================
    // Application Use Cases - Aggregator
    // ==========================================
    factory { AddAggregatorUsecase(get()) }
    factory { ListAggregatorUsecase(get()) }
    factory { ListAllActiveAggregatorUsecase(get()) }
    factory { ListGameVariantsUsecase(get(), get()) }
    factory { SyncGameUsecase(get(), get(), get(), get(), get(), get()) }
}

/**
 * Extension to get the core module for an Application.
 */
val Application.gameCoreModule get() = coreModule()
