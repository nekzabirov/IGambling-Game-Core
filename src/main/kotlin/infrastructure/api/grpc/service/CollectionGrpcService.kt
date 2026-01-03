package infrastructure.api.grpc.service

import application.port.inbound.CommandHandler
import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.collection.command.UpdateCollectionCommand
import com.nekgamebling.application.port.inbound.collection.command.UpdateCollectionGamesCommand
import com.nekgamebling.application.port.inbound.collection.query.FindAllCollectionsQuery
import com.nekgamebling.application.port.inbound.collection.query.FindAllCollectionsResponse
import com.nekgamebling.application.port.inbound.collection.query.FindCollectionQuery
import com.nekgamebling.application.port.inbound.collection.query.FindCollectionResponse
import com.nekgamebling.game.dto.PaginationMetaDto
import com.nekgamebling.game.service.CollectionItemDto
import com.nekgamebling.game.service.CollectionServiceGrpcKt
import com.nekgamebling.game.service.FindAllCollectionResult
import com.nekgamebling.game.service.FindCollectionResult
import com.nekgamebling.game.service.UpdateCollectionResult
import com.nekgamebling.game.service.UpdateCollectionGamesResult
import infrastructure.api.grpc.mapper.toProto
import io.grpc.Status
import io.grpc.StatusException
import shared.value.Pageable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import com.nekgamebling.game.service.FindCollectionQuery as FindCollectionQueryProto
import com.nekgamebling.game.service.FindAllCollectionQuery as FindAllCollectionQueryProto
import com.nekgamebling.game.service.UpdateCollectionCommand as UpdateCollectionCommandProto
import com.nekgamebling.game.service.UpdateCollectionGamesCommand as UpdateCollectionGamesCommandProto

class CollectionGrpcService(
    private val findCollectionQueryHandler: QueryHandler<FindCollectionQuery, FindCollectionResponse>,
    private val findAllCollectionsQueryHandler: QueryHandler<FindAllCollectionsQuery, FindAllCollectionsResponse>,
    private val updateCollectionCommandHandler: CommandHandler<UpdateCollectionCommand, Unit>,
    private val updateCollectionGamesCommandHandler: CommandHandler<UpdateCollectionGamesCommand, Unit>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
) : CollectionServiceGrpcKt.CollectionServiceCoroutineImplBase(coroutineContext) {

    override suspend fun find(request: FindCollectionQueryProto): FindCollectionResult {
        val query = FindCollectionQuery(identity = request.identity)

        return findCollectionQueryHandler.handle(query)
            .map { response ->
                FindCollectionResult.newBuilder()
                    .setCollection(response.collection.toProto())
                    .setProviderCount(response.providerCount)
                    .setGameCount(response.gameCount)
                    .build()
            }
            .getOrElse { error ->
                throw StatusException(
                    Status.NOT_FOUND.withDescription(error.message)
                )
            }
    }

    override suspend fun findAll(request: FindAllCollectionQueryProto): FindAllCollectionResult {
        val query = FindAllCollectionsQuery(
            pageable = if (request.hasPagination()) {
                Pageable(
                    page = request.pagination.page,
                    size = request.pagination.size
                )
            } else {
                Pageable.DEFAULT
            },
            query = request.query,
            active = if (request.hasActive()) request.active else null
        )

        return findAllCollectionsQueryHandler.handle(query)
            .map { response ->
                FindAllCollectionResult.newBuilder()
                    .addAllItems(response.result.items.map { item ->
                        CollectionItemDto.newBuilder()
                            .setCollection(item.collection.toProto())
                            .setProviderCount(item.providerCount)
                            .setGameCount(item.gameCount)
                            .build()
                    })
                    .setPagination(
                        PaginationMetaDto.newBuilder()
                            .setPage(response.result.currentPage)
                            .setSize(query.pageable.sizeReal)
                            .setTotalElements(response.result.totalItems)
                            .setTotalPages(response.result.totalPages.toInt())
                            .build()
                    )
                    .build()
            }
            .getOrElse { error ->
                throw StatusException(
                    Status.INTERNAL.withDescription(error.message)
                )
            }
    }

    override suspend fun update(request: UpdateCollectionCommandProto): UpdateCollectionResult {
        val command = UpdateCollectionCommand(
            identity = request.identity,
            active = if (request.hasActive()) request.active else null,
            order = if (request.hasOrder()) request.order else null
        )

        return updateCollectionCommandHandler.handle(command)
            .map { UpdateCollectionResult.newBuilder().build() }
            .getOrElse { error ->
                throw StatusException(
                    Status.NOT_FOUND.withDescription(error.message)
                )
            }
    }

    override suspend fun updateGames(request: UpdateCollectionGamesCommandProto): UpdateCollectionGamesResult {
        val command = UpdateCollectionGamesCommand(
            identity = request.identity,
            addGames = request.addGamesList,
            removeGames = request.removeGamesList
        )

        return updateCollectionGamesCommandHandler.handle(command)
            .map { UpdateCollectionGamesResult.newBuilder().build() }
            .getOrElse { error ->
                throw StatusException(
                    Status.NOT_FOUND.withDescription(error.message)
                )
            }
    }
}
