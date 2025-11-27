package service

import com.nekzabirov.igambling.proto.dto.EmptyResult
import com.nekzabirov.igambling.proto.service.AddCollectionCommand
import com.nekzabirov.igambling.proto.service.AddGameCollectionCommand
import com.nekzabirov.igambling.proto.service.ChangeGameOrderCollectionCommand
import com.nekzabirov.igambling.proto.service.CollectionGrpcKt
import com.nekzabirov.igambling.proto.service.ListCollectionCommand
import com.nekzabirov.igambling.proto.service.ListCollectionResult
import com.nekzabirov.igambling.proto.service.UpdateCollectionCommand
import core.value.LocaleName
import core.model.Pageable
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.Application
import mapper.toCollectionProto
import org.koin.ktor.ext.get
import app.usecase.AddCollectionUsecase
import app.usecase.AddGameCollectionUsecase
import app.usecase.ChangeGameOrderUsecase
import app.usecase.ListCollectionUsecase
import app.usecase.RemoveGameCollectionUsecase
import app.usecase.UpdateCollectionUsecase

class CollectionServiceImpl(application: Application) : CollectionGrpcKt.CollectionCoroutineImplBase() {
    private val addCollectionUsecase = application.get<AddCollectionUsecase>()
    private val updateCollectionUsecase = application.get<UpdateCollectionUsecase>()
    private val addGameCollectionUsecase = application.get<AddGameCollectionUsecase>()
    private val changeGameOrderUsecase = application.get<ChangeGameOrderUsecase>()
    private val removeGameCollectionUsecase = application.get<RemoveGameCollectionUsecase>()
    private val listCollectionUsecase = application.get<ListCollectionUsecase>()

    override suspend fun addCollection(request: AddCollectionCommand): EmptyResult {
        addCollectionUsecase(request.identity, LocaleName(request.nameMap))
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

        return EmptyResult.getDefaultInstance()
    }

    override suspend fun updateCollection(request: UpdateCollectionCommand): EmptyResult {
        updateCollectionUsecase(
            identity = request.identity,
            name = LocaleName(request.nameMap),
            order = request.order,
            active = request.active
        ).getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

        return EmptyResult.getDefaultInstance()
    }

    override suspend fun addGameCollection(request: AddGameCollectionCommand): EmptyResult {
        addGameCollectionUsecase(request.identity, request.gameIdentity)
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

        return EmptyResult.getDefaultInstance()
    }

    override suspend fun changeGameOrder(request: ChangeGameOrderCollectionCommand): EmptyResult {
        changeGameOrderUsecase(
            collectionIdentity = request.identity,
            gameIdentity = request.gameIdentity,
            order = request.order
        ).getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

        return EmptyResult.getDefaultInstance()
    }

    override suspend fun removeGameFromCollection(request: AddGameCollectionCommand): EmptyResult {
        removeGameCollectionUsecase(request.identity, request.gameIdentity)
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

        return EmptyResult.getDefaultInstance()
    }

    override suspend fun list(request: ListCollectionCommand): ListCollectionResult {
        val page = listCollectionUsecase(pageable = Pageable(request.pageNumber, request.pageSize)) {
            it.withQuery(request.query)

            if (request.hasActive()) {
                it.withActive(request.active)
            }
        }
            .map {
                ListCollectionResult.Item.newBuilder()
                    .setTotalGames(it.totalGamesCount)
                    .setActiveGames(it.activeGamesCount)
                    .setCollection(it.category.toCollectionProto())
                    .build()
            }

        return ListCollectionResult.newBuilder()
            .setTotalPage(page.totalPages.toInt())
            .addAllItems(page.items)
            .build()
    }
}