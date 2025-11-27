package service

import com.nekzabirov.igambling.proto.dto.EmptyResult
import app.usecase.AddAggregatorUsecase
import app.usecase.ListAggregatorUsecase
import app.usecase.ListGameVariantsUsecase
import com.nekzabirov.igambling.proto.service.AddAggregatorCommand
import com.nekzabirov.igambling.proto.service.AssignProviderCommand
import com.nekzabirov.igambling.proto.service.ListAggregatorCommand
import com.nekzabirov.igambling.proto.service.ListAggregatorResult
import com.nekzabirov.igambling.proto.service.ListVariantResult
import com.nekzabirov.igambling.proto.service.ListVariantsCommand
import com.nekzabirov.igambling.proto.service.SyncGrpcKt
import domain.aggregator.model.AggregatorInfo
import domain.aggregator.model.Aggregator
import core.model.Pageable
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.*
import mapper.toAggregatorProto
import mapper.toGameProto
import mapper.toGameVariantProto
import mapper.toProviderProto
import org.koin.ktor.ext.get
import app.usecase.AssignProviderToAggregatorUsecase
import java.util.UUID
import kotlin.collections.map

class SyncServiceImpl(application: Application) : SyncGrpcKt.SyncCoroutineImplBase() {
    private val addAggregatorUsecase = application.get<AddAggregatorUsecase>()
    private val listAggregatorUsecase = application.get<ListAggregatorUsecase>()
    private val listGameVariantsUsecase = application.get<ListGameVariantsUsecase>()
    private val assignProviderToAggregatorUsecase = application.get<AssignProviderToAggregatorUsecase>()

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
        }
            .let {
                ListVariantResult.newBuilder()
                    .setTotalPage(it.totalPages.toInt())
                    .addAllItems(it.items.map { i -> i.gameVariant }.map { v -> v.toGameVariantProto() })
                    .addAllGames(it.items.map { i -> i.game.game }.toSet().map { g -> g.toGameProto() })
                    .addAllProviders(it.items.map { i -> i.game.provider }.toSet().map { p -> p.toProviderProto() })
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
}