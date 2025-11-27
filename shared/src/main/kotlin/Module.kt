import app.adapter.CacheAdapter
import app.adapter.CurrencyAdapter
import app.adapter.EventProducerAdapter
import app.adapter.PlayerAdapter
import app.adapter.WalletAdapter
import app.service.spin.FreeSpinService
import app.service.spin.SpinService
import app.service.spin.SpinServiceSpec
import infrastructure.adapter.BaseCurrencyAdapter
import app.usecase.AddAggregatorUsecase
import app.usecase.ListAggregatorUsecase
import app.usecase.ListAllActiveAggregatorUsecase
import app.usecase.ListCollectionUsecase
import app.usecase.ListGameVariantsUsecase
import app.usecase.SyncGameUsecase
import io.ktor.server.application.Application
import org.koin.dsl.module
import app.usecase.AddCollectionUsecase
import app.usecase.AddGameCollectionUsecase
import app.usecase.AddGameFavouriteUsecase
import app.usecase.AddGameTagUsecase
import app.usecase.AddGameWonUsecase
import app.usecase.AssignProviderToAggregatorUsecase
import app.usecase.CancelFreespinUsecase
import app.usecase.ChangeGameOrderUsecase
import app.usecase.CloseRoundUsecase
import app.usecase.CreateFreespinUsecase
import app.usecase.DemoGameUsecase
import app.usecase.FindPlayerGameBalance
import app.usecase.GetPresetUsecase
import app.usecase.ListGameUsecase
import app.usecase.OpenSessionUsecase
import app.usecase.PlaceSpinUsecase
import app.usecase.ProviderListUsecase
import app.usecase.RemoveGameCollectionUsecase
import app.usecase.RemoveGameFavouriteUsecase
import app.usecase.RemoveGameTagUsecase
import app.usecase.SettleSpinUsecase
import app.usecase.UpdateCollectionUsecase
import app.usecase.UpdateGameUsecase
import app.usecase.UpdateProviderUsecase
import infrastructure.adapter.FakePlayerAdapter
import infrastructure.adapter.FakeWalletAdapter
import infrastructure.adapter.MapCacheAdapter
import infrastructure.adapter.RabbitMqEventProducer

val Application.sharedModule
    get() = module {
        single<CurrencyAdapter> { BaseCurrencyAdapter() }
        single<WalletAdapter> { FakeWalletAdapter() }
        single<PlayerAdapter> { FakePlayerAdapter() }
        single<CacheAdapter> { MapCacheAdapter() }
        single<EventProducerAdapter> { RabbitMqEventProducer(this@sharedModule) }

        single { SpinService() }
        single { FreeSpinService() }
        single { SpinServiceSpec() }

        //Usecase
        factory { AddAggregatorUsecase() }
        factory { ListAllActiveAggregatorUsecase() }
        factory { ListCollectionUsecase() }
        factory { SyncGameUsecase() }
        factory { ListAggregatorUsecase() }
        factory { ListGameVariantsUsecase() }
        factory { AssignProviderToAggregatorUsecase() }
        factory { AddCollectionUsecase() }
        factory { UpdateCollectionUsecase() }
        factory { AddGameCollectionUsecase() }
        factory { ChangeGameOrderUsecase() }
        factory { RemoveGameCollectionUsecase() }
        factory { ProviderListUsecase() }
        factory { UpdateProviderUsecase() }
        factory { UpdateGameUsecase() }
        factory { AddGameTagUsecase() }
        factory { RemoveGameTagUsecase() }
        factory { AddGameFavouriteUsecase() }
        factory { RemoveGameFavouriteUsecase() }
        factory { OpenSessionUsecase() }
        factory { GetPresetUsecase() }
        factory { CreateFreespinUsecase() }
        factory { CancelFreespinUsecase() }
        factory { ListGameUsecase() }
        factory { DemoGameUsecase() }
        factory { FindPlayerGameBalance() }
        factory { PlaceSpinUsecase() }
        factory { SettleSpinUsecase() }
        factory { CloseRoundUsecase() }
        factory { AddGameWonUsecase() }
    }