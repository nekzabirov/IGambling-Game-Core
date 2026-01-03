package infrastructure.api.grpc.service

import application.port.inbound.command.*
import application.port.inbound.query.*
import application.service.GameSyncService
import domain.common.value.Aggregator
import domain.game.repository.GameVariantFilter
import infrastructure.handler.command.*
import infrastructure.handler.query.*
import shared.value.Pageable
import com.nekzabirov.igambling.proto.dto.EmptyResult
import com.nekzabirov.igambling.proto.service.AddAggregatorCommand
import com.nekzabirov.igambling.proto.service.AssignProviderCommand
import com.nekzabirov.igambling.proto.service.ListAggregatorCommand
import com.nekzabirov.igambling.proto.service.ListAggregatorResult
import com.nekzabirov.igambling.proto.service.ListVariantResult
import com.nekzabirov.igambling.proto.service.ListVariantsCommand
import com.nekzabirov.igambling.proto.service.SyncGameCommand
import com.nekzabirov.igambling.proto.service.SyncGrpcKt
import com.nekzabirov.igambling.proto.service.SyncGameResult as SyncGameResultProto
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.*
import infrastructure.api.grpc.mapper.toAggregatorProto
import infrastructure.api.grpc.mapper.toGameProto
import infrastructure.api.grpc.mapper.toGameVariantProto
import infrastructure.api.grpc.mapper.toProviderProto
import org.koin.ktor.ext.get

class SyncServiceImpl(application: Application) : SyncGrpcKt.SyncCoroutineImplBase() {
    private val addAggregatorCommandHandler = application.get<AddAggregatorCommandHandler>()
    private val listAggregatorsQueryHandler = application.get<ListAggregatorsQueryHandler>()
    private val listGameVariantsQueryHandler = application.get<ListGameVariantsQueryHandler>()
    private val assignProviderToAggregatorCommandHandler = application.get<AssignProviderToAggregatorCommandHandler>()
    private val gameSyncService = application.get<GameSyncService>()

    override suspend fun addAggregator(request: AddAggregatorCommand): EmptyResult {
        val type = Aggregator.valueOf(request.type)

        return addAggregatorCommandHandler.handle(
            application.port.inbound.command.AddAggregatorCommand(
                identity = request.identity,
                aggregator = type,
                config = request.configMap
            )
        )
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }
    }

    override suspend fun listAggregator(request: ListAggregatorCommand): ListAggregatorResult {
        val pageable = Pageable(page = request.pageNumber, size = request.pageSize)

        val page = listAggregatorsQueryHandler.handle(
            ListAggregatorsQuery(
                pageable = pageable,
                query = request.query,
                active = if (request.hasActive()) request.active else null,
                type = if (request.hasType()) Aggregator.valueOf(request.type) else null
            )
        )

        return ListAggregatorResult.newBuilder()
            .setTotalPage(page.totalPages.toInt())
            .addAllItems(page.items.map { a ->
                com.nekzabirov.igambling.proto.dto.AggregatorDto.newBuilder()
                    .setId(a.id.toString())
                    .setIdentity(a.identity)
                    .setType(a.aggregator.name)
                    .setActive(a.active)
                    .build()
            })
            .build()
    }

    override suspend fun listVariants(request: ListVariantsCommand): ListVariantResult {
        val pageable = Pageable(page = request.pageNumber, size = request.pageSize)
        val filter = GameVariantFilter.Builder().apply {
            withQuery(request.query)

            if (request.hasAggregatorType()) {
                withAggregator(Aggregator.valueOf(request.aggregatorType))
            }

            if (request.hasGameIdentity()) {
                withGameIdentity(request.gameIdentity)
            }
        }.build()

        val page = listGameVariantsQueryHandler.handle(
            ListGameVariantsQuery(pageable = pageable, filter = filter)
        )

        return ListVariantResult.newBuilder()
            .setTotalPage(page.totalPages.toInt())
            .addAllItems(page.items.map { i -> i.variant }.map { v -> v.toGameVariantProto() })
            .addAllGames(page.items.map { i -> i.game }.toSet().mapNotNull { g -> g?.toGameProto() })
            .addAllProviders(page.items.map { i -> i.provider }.toSet().mapNotNull { p -> p?.toProviderProto() })
            .build()
    }

    override suspend fun assignProvider(request: AssignProviderCommand): EmptyResult {
        assignProviderToAggregatorCommandHandler.handle(
            AssignProviderToAggregatorCommand(
                providerIdentity = request.providerId,
                aggregatorIdentity = request.aggregatorIdentity
            )
        ).getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

        return EmptyResult.getDefaultInstance()
    }

    override suspend fun syncGame(request: SyncGameCommand): SyncGameResultProto {
        return gameSyncService.sync(request.aggregatorIdentity)
            .map { result ->
                SyncGameResultProto.newBuilder()
                    .setGameCount(result.gameCount)
                    .setProviderCount(result.providerCount)
                    .build()
            }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }
    }
}
