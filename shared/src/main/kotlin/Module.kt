import usecase.AddAggregatorUsecase
import usecase.ListAggregatorUsecase
import usecase.ListAllActiveAggregatorUsecase
import usecase.ListCollectionUsecase
import usecase.ListGameVariantsUsecase
import usecase.SyncGameUsecase
import io.ktor.server.application.Application
import org.koin.dsl.module
import usecase.AddCollectionUsecase
import usecase.AddGameCollectionUsecase
import usecase.AssignProviderToAggregatorUsecase
import usecase.ChangeGameOrderUsecase
import usecase.ProviderListUsecase
import usecase.RemoveGameCollectionUsecase
import usecase.UpdateCollectionUsecase
import usecase.UpdateProviderUsecase

val Application.sharedModule
    get() = module {
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
    }