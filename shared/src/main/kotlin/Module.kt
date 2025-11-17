import app.adapter.CurrencyAdapter
import app.adapter.PlayerAdapter
import app.adapter.WalletAdapter
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
import app.usecase.AssignProviderToAggregatorUsecase
import app.usecase.CancelFreespinUsecase
import app.usecase.ChangeGameOrderUsecase
import app.usecase.CreateFreespinUsecase
import app.usecase.DemoGameUsecase
import app.usecase.GetPresetUsecase
import app.usecase.ListGameUsecase
import app.usecase.OpenSessionUsecase
import app.usecase.ProviderListUsecase
import app.usecase.RemoveGameCollectionUsecase
import app.usecase.RemoveGameFavouriteUsecase
import app.usecase.RemoveGameTagUsecase
import app.usecase.UpdateCollectionUsecase
import app.usecase.UpdateGameUsecase
import app.usecase.UpdateProviderUsecase
import infrastructure.adapter.FakePlayerAdapter
import infrastructure.adapter.FakeWalletAdapter

val Application.sharedModule
    get() = module {
        single<CurrencyAdapter> { BaseCurrencyAdapter() }
        single<WalletAdapter> { FakeWalletAdapter() }
        single<PlayerAdapter> { FakePlayerAdapter() }

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
    }