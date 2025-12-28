package infrastructure.api.grpc.service

import application.usecase.aggregator.AddAggregatorUsecase
import application.usecase.aggregator.ListAggregatorUsecase
import application.usecase.aggregator.ListGameVariantsUsecase
import application.usecase.aggregator.SyncGameUsecase
import application.usecase.provider.AssignProviderToAggregatorUsecase
import domain.aggregator.model.AggregatorInfo
import shared.value.Aggregator
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
import java.util.UUID

class SyncServiceImpl(application: Application) : SyncGrpcKt.SyncCoroutineImplBase() {
    private val addAggregatorUsecase = application.get<AddAggregatorUsecase>()
    private val listAggregatorUsecase = application.get<ListAggregatorUsecase>()
    private val listGameVariantsUsecase = application.get<ListGameVariantsUsecase>()
    private val assignProviderToAggregatorUsecase = application.get<AssignProviderToAggregatorUsecase>()
    private val syncGameUsecase = application.get<SyncGameUsecase>()

    override suspend fun addAggregator(request: AddAggregatorCommand): EmptyResult {
        val type = Aggregator.valueOf(request.type)

        return addAggregatorUsecase(request.identity, type, request.configMap)
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }
    }

    override suspend fun listAggregator(request: ListAggregatorCommand): ListAggregatorResult {
        val pageable = Pageable(page = request.pageNumber, size = request.pageSize)

        return listAggregatorUsecase(pageable) {
            it.withQuery(request.query)

            if (request.hasActive()) {
                it.withActive(request.active)
            }

            if (request.hasType()) {
                it.withActive(request.active)
            }
        }
            .let {
                ListAggregatorResult.newBuilder()
                    .setTotalPage(it.totalPages.toInt())
                    .addAllItems(it.items.map { a: AggregatorInfo -> a.toAggregatorProto() })
                    .build()
            }
    }

    override suspend fun listVariants(request: ListVariantsCommand): ListVariantResult {
        val page = Pageable(page = request.pageNumber, size = request.pageSize)

        return listGameVariantsUsecase(page) {
            withQuery(request.query)

            if (request.hasAggregatorType()) {
                withAggregator(Aggregator.valueOf(request.aggregatorType))
            }

            if (request.hasGameIdentity()) {
                withGameIdentity(request.gameIdentity)
            }
        }.let {
            ListVariantResult.newBuilder()
                .setTotalPage(it.totalPages.toInt())
                .addAllItems(it.items.map { i -> i.variant }.map { v -> v.toGameVariantProto() })
                .addAllGames(it.items.map { i -> i.game }.toSet().mapNotNull { g -> g?.toGameProto() })
                .addAllProviders(it.items.map { i -> i.provider }.toSet().mapNotNull { p -> p?.toProviderProto() })
                .build()
        }
    }

    override suspend fun assignProvider(request: AssignProviderCommand): EmptyResult {
        assignProviderToAggregatorUsecase(
            providerId = UUID.fromString(request.providerId),
            aggregatorId = UUID.fromString(request.providerId)
        ).getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

        return EmptyResult.getDefaultInstance()
    }

    override suspend fun syncGame(request: SyncGameCommand): SyncGameResultProto {
        return syncGameUsecase(request.aggregatorIdentity)
            .map { result ->
                SyncGameResultProto.newBuilder()
                    .setGameCount(result.gameCount)
                    .setProviderCount(result.providerCount)
                    .build()
            }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }
    }
}