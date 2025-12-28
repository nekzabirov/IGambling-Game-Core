package infrastructure.api.grpc.service

import application.usecase.provider.ProviderListUsecase
import application.usecase.provider.UpdateProviderUsecase
import shared.value.Pageable
import com.nekzabirov.igambling.proto.dto.EmptyResult
import com.nekzabirov.igambling.proto.service.ListProviderCommand
import com.nekzabirov.igambling.proto.service.ListProviderResult
import com.nekzabirov.igambling.proto.service.ProviderGrpcKt
import com.nekzabirov.igambling.proto.service.UpdateProviderConfig
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.*
import infrastructure.api.grpc.mapper.toAggregatorProto
import infrastructure.api.grpc.mapper.toProviderProto
import org.koin.ktor.ext.get

class ProviderServiceImpl(application: Application) : ProviderGrpcKt.ProviderCoroutineImplBase() {
    private val providerListUsecase = application.get<ProviderListUsecase>()
    private val updateProviderUsecase = application.get<UpdateProviderUsecase>()

    override suspend fun list(request: ListProviderCommand): ListProviderResult {
        val page = providerListUsecase(pageable = Pageable(request.pageNumber, request.pageSize)) { builder ->
            builder.withQuery(request.query)

            if (request.hasActive()) {
                builder.withActive(request.active)
            }
        }

        val items = page.items.map { i ->
            ListProviderResult.Item
                .newBuilder()
                .setActiveGames(i.activeGamesCount)
                .setTotalGames(i.totalGamesCount)
                .setProvider(i.provider.toProviderProto())
                .build()
        }

        val aggregators = page.items
            .map { i -> i.aggregatorInfo }
            .toSet()
            .map { i -> i.toAggregatorProto() }

        return ListProviderResult.newBuilder()
            .setTotalPage(page.totalPages.toInt())
            .addAllItems(items)
            .addAllAggregators(aggregators)
            .build()
    }

    override suspend fun update(request: UpdateProviderConfig): EmptyResult =
        updateProviderUsecase(identity = request.identity, order = request.order, active = request.active)
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }
}