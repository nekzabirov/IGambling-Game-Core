package service

import com.nekzabirov.igambling.proto.dto.EmptyResult
import com.nekzabirov.igambling.proto.service.ListProviderCommand
import com.nekzabirov.igambling.proto.service.ListProviderResult
import com.nekzabirov.igambling.proto.service.ProviderGrpcKt
import com.nekzabirov.igambling.proto.service.UpdateProviderConfig
import core.model.Pageable
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.*
import mapper.toAggregatorProto
import mapper.toProviderProto
import org.koin.ktor.ext.get
import app.usecase.ProviderListUsecase
import app.usecase.UpdateProviderUsecase

class ProviderServiceImpl(application: Application) : ProviderGrpcKt.ProviderCoroutineImplBase() {
    private val providerListUsecase = application.get<ProviderListUsecase>()
    private val updateProviderUsecase = application.get<UpdateProviderUsecase>()

    override suspend fun list(request: ListProviderCommand): ListProviderResult =
        providerListUsecase(pageable = Pageable(request.pageNumber, request.pageSize)) {
            withQuery(request.query)

            if (request.hasActive()) {
                withActive(request.active)
            }
        }
            .let {
                val items = it.items.map { i ->
                    ListProviderResult.Item
                        .newBuilder()
                        .setActiveGames(i.activeGamesCount)
                        .setTotalGames(i.totalGamesCount)
                        .setProvider(i.provider.toProviderProto())
                        .build()
                }

                val aggregators = it.items
                    .map { i -> i.aggregatorInfo }
                    .toSet()
                    .map { i -> i.toAggregatorProto() }

                ListProviderResult.newBuilder()
                    .setTotalPage(it.totalPages.toInt())
                    .addAllItems(items)
                    .addAllAggregators(aggregators)
                    .build()
            }

    override suspend fun update(request: UpdateProviderConfig): EmptyResult =
        updateProviderUsecase(identity = request.identity, order = request.order, active = request.active)
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }
}