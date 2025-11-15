package service

import com.nekzabirov.igambling.proto.dto.EmptyResult
import com.nekzabirov.igambling.proto.service.AddCollectionCommand
import com.nekzabirov.igambling.proto.service.CollectionGrpcKt
import com.nekzabirov.igambling.proto.service.UpdateCollectionCommand
import core.value.LocaleName
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.Application
import org.koin.ktor.ext.get
import usecase.AddCollectionUsecase
import usecase.UpdateCollectionUsecase


class CollectionServiceImpl(application: Application) : CollectionGrpcKt.CollectionCoroutineImplBase() {
    private val addCollectionUsecase = application.get<AddCollectionUsecase>()
    private val updateCollectionUsecase = application.get<UpdateCollectionUsecase>()

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
}