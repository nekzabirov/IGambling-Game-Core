package infrastructure

import application.port.outbound.*
import application.saga.spin.end.EndSpinSaga
import application.saga.spin.place.PlaceSpinSaga
import application.saga.spin.rollback.RollbackSpinSaga
import application.saga.spin.settle.SettleSpinSaga
import application.service.*
import infrastructure.handler.command.*
import infrastructure.handler.query.*
import infrastructure.messaging.messagingModule
import infrastructure.external.UnitCurrencyAdapter
import infrastructure.external.s3.S3FileAdapter
import infrastructure.external.turbo.TurboPlayerAdapter
import infrastructure.external.turbo.TurboWalletAdapter
import infrastructure.aggregator.AggregatorModule
import infrastructure.persistence.DBModule
import io.ktor.server.application.*
import org.koin.dsl.module

/**
 * Koin module for dependency injection.
 * All dependencies use constructor injection.
 */
fun Application.coreModule() = module {
    includes(
        DBModule,
        adapterModule,
        serviceModule,
        handlerModule,
        sagaModule,
        AggregatorModule,
        messagingModule(this@coreModule)
    )
}

private val adapterModule = module {
    // ==========================================
    // Infrastructure - Ports/Adapters
    // ==========================================
    single<WalletAdapter> { TurboWalletAdapter() }
    single<PlayerAdapter> { TurboPlayerAdapter() }
    single<CurrencyAdapter> { UnitCurrencyAdapter() }
    single<FileAdapter> {
        S3FileAdapter(
            endpoint = System.getenv("S3_ENDPOINT") ?: "http://localhost:9000",
            accessKey = System.getenv("S3_ACCESS_KEY") ?: "minioadmin",
            secretKey = System.getenv("S3_SECRET_KEY") ?: "minioadmin",
            bucketName = System.getenv("S3_BUCKET") ?: "uploads",
            region = System.getenv("S3_REGION") ?: "us-east-1"
        )
    }
}

private val serviceModule = module {
    // ==========================================
    // Application Services
    // ==========================================
    single { GameService(get(), get(), get()) }
    single { SessionService(get(), get(), get(), get(), get()) }
    single { SpinService(get(), get()) }
    single { AggregatorService(get()) }
    single { FreespinService(get(), get()) }
    single { GameSyncService(get(), get()) }
}

private val handlerModule = module {
    // ==========================================
    // Query Handlers - Game
    // ==========================================
    factory { FindGameByIdQueryHandler() }
    factory { FindGameByIdentityQueryHandler() }
    factory { FindGameBySymbolQueryHandler() }
    factory { ListGamesQueryHandler() }
    factory { SearchGamesQueryHandler() }

    // ==========================================
    // Query Handlers - Collection
    // ==========================================
    factory { ListCollectionsQueryHandler() }
    factory { FindCollectionByIdentityQueryHandler() }
    factory { FindCollectionByIdQueryHandler() }

    // ==========================================
    // Query Handlers - Provider
    // ==========================================
    factory { ListProvidersQueryHandler() }
    factory { FindProviderByIdentityQueryHandler() }
    factory { FindProviderByIdQueryHandler() }

    // ==========================================
    // Query Handlers - Aggregator
    // ==========================================
    factory { ListAggregatorsQueryHandler() }
    factory { ListActiveAggregatorsQueryHandler() }
    factory { FindAggregatorByIdentityQueryHandler() }
    factory { FindAggregatorByIdQueryHandler() }
    factory { FindAggregatorByTypeQueryHandler() }
    factory { ListGameVariantsQueryHandler() }

    // ==========================================
    // Query Handlers - Round
    // ==========================================
    factory { GetRoundsDetailsQueryHandler() }

    // ==========================================
    // Command Handlers - Game
    // ==========================================
    factory { UpdateGameCommandHandler() }
    factory { UpdateGameImageCommandHandler(get()) }
    factory { AddGameTagCommandHandler() }
    factory { RemoveGameTagCommandHandler() }
    factory { AddGameFavouriteCommandHandler(get()) }
    factory { RemoveGameFavouriteCommandHandler(get()) }
    factory { AddGameWinCommandHandler(get()) }

    // ==========================================
    // Command Handlers - Collection
    // ==========================================
    factory { AddCollectionCommandHandler() }
    factory { UpdateCollectionCommandHandler() }
    factory { AddGameToCollectionCommandHandler() }
    factory { RemoveGameFromCollectionCommandHandler() }
    factory { ChangeGameOrderInCollectionCommandHandler() }

    // ==========================================
    // Command Handlers - Provider
    // ==========================================
    factory { UpdateProviderCommandHandler() }
    factory { UpdateProviderImageCommandHandler(get()) }
    factory { AssignProviderToAggregatorCommandHandler() }

    // ==========================================
    // Command Handlers - Aggregator
    // ==========================================
    factory { AddAggregatorCommandHandler() }
}

private val sagaModule = module {
    // ==========================================
    // Application Sagas - Distributed Transactions
    // ==========================================
    factory { PlaceSpinSaga(get(), get(), get(), get(), get()) }
    factory { SettleSpinSaga(get(), get(), get()) }
    factory { EndSpinSaga(get(), get()) }
    factory { RollbackSpinSaga(get(), get(), get()) }
}

/**
 * Extension to get the core module for an Application.
 */
val Application.gameCoreModule get() = coreModule()
