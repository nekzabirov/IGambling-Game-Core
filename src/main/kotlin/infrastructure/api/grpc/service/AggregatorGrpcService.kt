package infrastructure.api.grpc.service

import application.port.inbound.CommandHandler
import application.port.inbound.QueryHandler
import com.nekgamebling.application.port.inbound.aggregator.CreateAggregatorCommand
import com.nekgamebling.application.port.inbound.aggregator.CreateAggregatorResponse
import com.nekgamebling.application.port.inbound.aggregator.FindAggregatorQuery
import com.nekgamebling.application.port.inbound.aggregator.FindAggregatorResponse
import com.nekgamebling.application.port.inbound.aggregator.FindAllAggregatorQuery
import com.nekgamebling.application.port.inbound.aggregator.FindAllAggregatorResponse
import com.nekgamebling.application.port.inbound.aggregator.UpdateAggregatorCommand
import com.nekgamebling.game.dto.PaginationMetaDto
import com.nekgamebling.game.service.AggregatorServiceGrpcKt
import com.nekgamebling.game.service.CreateAggregatorResult
import com.nekgamebling.game.service.FindAggregatorResult
import com.nekgamebling.game.service.FindAllAggregatorResult
import com.nekgamebling.game.service.UpdateAggregatorResult
import infrastructure.api.grpc.error.mapOrThrowGrpc
import infrastructure.api.grpc.mapper.toDomain
import infrastructure.api.grpc.mapper.toProto
import shared.value.Pageable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import com.nekgamebling.game.service.CreateAggregatorCommand as CreateAggregatorCommandProto
import com.nekgamebling.game.service.FindAggregatorQuery as FindAggregatorQueryProto
import com.nekgamebling.game.service.FindAllAggregatorQuery as FindAllAggregatorQueryProto
import com.nekgamebling.game.service.UpdateAggregatorCommand as UpdateAggregatorCommandProto

class AggregatorGrpcService(
    private val createAggregatorCommandHandler: CommandHandler<CreateAggregatorCommand, CreateAggregatorResponse>,
    private val findAggregatorQueryHandler: QueryHandler<FindAggregatorQuery, FindAggregatorResponse>,
    private val findAllAggregatorQueryHandler: QueryHandler<FindAllAggregatorQuery, FindAllAggregatorResponse>,
    private val updateAggregatorCommandHandler: CommandHandler<UpdateAggregatorCommand, Unit>,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
) : AggregatorServiceGrpcKt.AggregatorServiceCoroutineImplBase(coroutineContext) {

    override suspend fun create(request: CreateAggregatorCommandProto): CreateAggregatorResult {
        val command = CreateAggregatorCommand(
            identity = request.identity,
            aggregator = request.aggregator.toDomain(),
            config = request.configMap,
            active = if (request.hasActive()) request.active else true
        )

        return createAggregatorCommandHandler.handle(command)
            .mapOrThrowGrpc { response ->
                CreateAggregatorResult.newBuilder()
                    .setAggregator(response.aggregator.toProto())
                    .build()
            }
    }

    override suspend fun find(request: FindAggregatorQueryProto): FindAggregatorResult {
        val query = FindAggregatorQuery(identity = request.identity)

        return findAggregatorQueryHandler.handle(query)
            .mapOrThrowGrpc { response ->
                FindAggregatorResult.newBuilder()
                    .setAggregator(response.aggregator.toProto())
                    .build()
            }
    }

    override suspend fun findAll(request: FindAllAggregatorQueryProto): FindAllAggregatorResult {
        val query = FindAllAggregatorQuery(
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

        return findAllAggregatorQueryHandler.handle(query)
            .mapOrThrowGrpc { response ->
                FindAllAggregatorResult.newBuilder()
                    .addAllItems(response.result.items.map { it.toProto() })
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
    }

    override suspend fun update(request: UpdateAggregatorCommandProto): UpdateAggregatorResult {
        val command = UpdateAggregatorCommand(
            identity = request.identity,
            active = if (request.hasActive()) request.active else null,
            config = request.configMap.takeIf { it.isNotEmpty() }
        )

        return updateAggregatorCommandHandler.handle(command)
            .mapOrThrowGrpc { UpdateAggregatorResult.newBuilder().build() }
    }
}
