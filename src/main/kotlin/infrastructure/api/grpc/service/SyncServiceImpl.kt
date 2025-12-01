package com.nekgamebling.infrastructure.api.grpc.service

import com.nekgamebling.application.usecase.aggregator.AddAggregatorUsecase
import com.nekgamebling.application.usecase.aggregator.ListAggregatorUsecase
import com.nekgamebling.application.usecase.aggregator.ListGameVariantsUsecase
import com.nekgamebling.application.usecase.provider.AssignProviderToAggregatorUsecase
import com.nekgamebling.domain.aggregator.model.AggregatorInfo
import com.nekgamebling.shared.value.Aggregator
import com.nekgamebling.shared.value.Pageable
import com.nekzabirov.igambling.proto.dto.EmptyResult
import com.nekzabirov.igambling.proto.service.AddAggregatorCommand
import com.nekzabirov.igambling.proto.service.AssignProviderCommand
import com.nekzabirov.igambling.proto.service.ListAggregatorCommand
import com.nekzabirov.igambling.proto.service.ListAggregatorResult
import com.nekzabirov.igambling.proto.service.ListVariantResult
import com.nekzabirov.igambling.proto.service.ListVariantsCommand
import com.nekzabirov.igambling.proto.service.SyncGrpcKt
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.*
import com.nekgamebling.infrastructure.api.grpc.mapper.toAggregatorProto
import com.nekgamebling.infrastructure.api.grpc.mapper.toGameProto
import com.nekgamebling.infrastructure.api.grpc.mapper.toGameVariantProto
import com.nekgamebling.infrastructure.api.grpc.mapper.toProviderProto
import org.koin.ktor.ext.get
import java.util.UUID

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
}