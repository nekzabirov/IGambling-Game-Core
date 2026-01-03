package infrastructure.api.grpc.service

import application.port.inbound.command.*
import application.port.inbound.query.*
import infrastructure.handler.command.*
import infrastructure.handler.query.*
import shared.value.LocaleName
import shared.value.Pageable
import com.nekzabirov.igambling.proto.dto.EmptyResult
import com.nekzabirov.igambling.proto.service.AddCollectionCommand
import com.nekzabirov.igambling.proto.service.AddGameCollectionCommand
import com.nekzabirov.igambling.proto.service.ChangeGameOrderCollectionCommand
import com.nekzabirov.igambling.proto.service.CollectionGrpcKt
import com.nekzabirov.igambling.proto.service.ListCollectionCommand
import com.nekzabirov.igambling.proto.service.ListCollectionResult
import com.nekzabirov.igambling.proto.service.UpdateCollectionCommand
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.Application
import infrastructure.api.grpc.mapper.toCollectionProto
import org.koin.ktor.ext.get

class CollectionServiceImpl(application: Application) : CollectionGrpcKt.CollectionCoroutineImplBase() {
    private val addCollectionCommandHandler = application.get<AddCollectionCommandHandler>()
    private val updateCollectionCommandHandler = application.get<UpdateCollectionCommandHandler>()
    private val addGameToCollectionCommandHandler = application.get<AddGameToCollectionCommandHandler>()
    private val changeGameOrderCommandHandler = application.get<ChangeGameOrderInCollectionCommandHandler>()
    private val removeGameFromCollectionCommandHandler = application.get<RemoveGameFromCollectionCommandHandler>()
    private val listCollectionsQueryHandler = application.get<ListCollectionsQueryHandler>()

    override suspend fun addCollection(request: AddCollectionCommand): EmptyResult {
        addCollectionCommandHandler.handle(
            application.port.inbound.command.AddCollectionCommand(
                identity = request.identity,
                name = LocaleName(request.nameMap)
            )
        ).getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

        return EmptyResult.getDefaultInstance()
    }

    override suspend fun updateCollection(request: UpdateCollectionCommand): EmptyResult {
        updateCollectionCommandHandler.handle(
            application.port.inbound.command.UpdateCollectionCommand(
                identity = request.identity,
                name = LocaleName(request.nameMap),
                order = request.order,
                active = request.active
            )
        ).getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

        return EmptyResult.getDefaultInstance()
    }

    override suspend fun addGameCollection(request: AddGameCollectionCommand): EmptyResult {
        addGameToCollectionCommandHandler.handle(
            AddGameToCollectionCommand(
                collectionIdentity = request.identity,
                gameIdentity = request.gameIdentity
            )
        ).getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

        return EmptyResult.getDefaultInstance()
    }

    override suspend fun changeGameOrder(request: ChangeGameOrderCollectionCommand): EmptyResult {
        changeGameOrderCommandHandler.handle(
            ChangeGameOrderInCollectionCommand(
                collectionIdentity = request.identity,
                gameIdentity = request.gameIdentity,
                newOrder = request.order
            )
        ).getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

        return EmptyResult.getDefaultInstance()
    }

    override suspend fun removeGameFromCollection(request: AddGameCollectionCommand): EmptyResult {
        removeGameFromCollectionCommandHandler.handle(
            RemoveGameFromCollectionCommand(
                collectionIdentity = request.identity,
                gameIdentity = request.gameIdentity
            )
        ).getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

        return EmptyResult.getDefaultInstance()
    }

    override suspend fun list(request: ListCollectionCommand): ListCollectionResult {
        val page = listCollectionsQueryHandler.handle(
            ListCollectionsQuery(
                pageable = Pageable(request.pageNumber, request.pageSize),
                activeOnly = if (request.hasActive()) request.active else false
            )
        )

        val items = page.items.map {
            ListCollectionResult.Item.newBuilder()
                .setTotalGames(it.gameCount)
                .setActiveGames(it.gameCount) // Using gameCount for both since we don't track active separately
                .setCollection(
                    com.nekzabirov.igambling.proto.dto.CollectionDto.newBuilder()
                        .setId(it.id.toString())
                        .setIdentity(it.identity)
                        .putAllName(mapOf("en" to it.name))
                        .setOrder(it.order)
                        .setActive(it.active)
                        .build()
                )
                .build()
        }

        return ListCollectionResult.newBuilder()
            .setTotalPage(page.totalPages.toInt())
            .addAllItems(items)
            .build()
    }
}
