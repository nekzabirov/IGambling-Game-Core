package infrastructure

import application.usecase.spin.GetPresetUsecase
import application.port.outbound.*
import application.service.GameService
import application.service.SessionService
import application.service.SpinService
import application.usecase.aggregator.*
import application.usecase.collection.*
import application.usecase.game.*
import application.usecase.provider.*
import application.usecase.session.OpenSessionUsecase
import application.usecase.spin.*
import com.nekgamebling.application.service.AggregatorService
import domain.aggregator.repository.AggregatorRepository
import domain.collection.repository.CollectionRepository
import domain.game.repository.*
import domain.provider.repository.ProviderRepository
import domain.session.repository.RoundRepository
import domain.session.repository.SessionRepository
import domain.session.repository.SpinRepository
import infrastructure.adapter.BaseCurrencyAdapter
import infrastructure.adapter.FakePlayerAdapter
import infrastructure.adapter.FakeWalletAdapter
import infrastructure.aggregator.AggregatorAdapterRegistryImpl
import infrastructure.aggregator.AggregatorModule
import infrastructure.aggregator.onegamehub.OneGameHubAdapterFactory
import infrastructure.persistence.cache.InMemoryCacheAdapter
import infrastructure.messaging.RabbitMqEventPublisher
import infrastructure.persistence.DBModule
import infrastructure.persistence.exposed.adapter.ExposedGameSyncPort
import infrastructure.persistence.exposed.repository.*
import io.ktor.server.application.*
import org.koin.dsl.module

/**
 * Koin module for dependency injection.
 * All dependencies use constructor injection.
 */
fun Application.coreModule() = module {
    includes(
        DBModule,
        adapterModule(),
        serviceModule,
        useCaseModule,
        AggregatorModule
    )
}

private fun Application.adapterModule() = module {
    // ==========================================
    // Infrastructure - Ports/Adapters
    // ==========================================
    single<WalletPort> { FakeWalletAdapter() }
    single<PlayerPort> { FakePlayerAdapter() }
    single<CurrencyPort> { BaseCurrencyAdapter() }
    single<EventPublisherPort> { RabbitMqEventPublisher(this@adapterModule) }
    // Aggregator Infrastructure - Registry Pattern
    single<AggregatorAdapterRegistry> {
        AggregatorAdapterRegistryImpl().apply {
            // Register all aggregator factories here
            register(OneGameHubAdapterFactory())
        }
    }
}

private val serviceModule = module {
    // ==========================================
    // Application Services
    // ==========================================
    single { GameService(get(), get()) }
    single { SessionService(get(), get()) }
    single { SpinService(get(), get(), get(), get()) }
    single { AggregatorService(get(), get()) }
}

private val useCaseModule = module {
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
    factory { PlaceSpinUsecase(get(), get(), get(), get(), get()) }
    factory { SettleSpinUsecase(get(), get(), get(), get()) }
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
    factory { ListGameVariantsUsecase(get()) }
    factory { SyncGameUsecase(get(), get(), get(), get(), get(), get()) }
}

/**
 * Extension to get the core module for an Application.
 */
val Application.gameCoreModule get() = coreModule()
