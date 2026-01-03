package infrastructure.api.grpc.service

import application.port.inbound.command.*
import application.port.inbound.query.*
import application.port.outbound.MediaFile
import domain.provider.repository.ProviderFilter
import infrastructure.handler.command.*
import infrastructure.handler.query.*
import shared.value.Pageable
import com.nekzabirov.igambling.proto.dto.EmptyResult
import com.nekzabirov.igambling.proto.service.ListProviderCommand
import com.nekzabirov.igambling.proto.service.ListProviderResult
import com.nekzabirov.igambling.proto.service.ProviderGrpcKt
import com.nekzabirov.igambling.proto.service.UpdateProviderConfig
import com.nekzabirov.igambling.proto.service.UpdateProviderImageCommand
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.*
import infrastructure.api.grpc.mapper.toAggregatorProto
import infrastructure.api.grpc.mapper.toProviderProto
import org.koin.ktor.ext.get

class ProviderServiceImpl(application: Application) : ProviderGrpcKt.ProviderCoroutineImplBase() {
    private val listProvidersQueryHandler = application.get<ListProvidersQueryHandler>()
    private val updateProviderCommandHandler = application.get<UpdateProviderCommandHandler>()
    private val updateProviderImageCommandHandler = application.get<UpdateProviderImageCommandHandler>()

    override suspend fun list(request: ListProviderCommand): ListProviderResult {
        val filter = ProviderFilter(
            query = request.query,
            active = if (request.hasActive()) request.active else null
        )
        val page = listProvidersQueryHandler.handle(
            ListProvidersQuery(
                pageable = Pageable(request.pageNumber, request.pageSize),
                filter = filter
            )
        )

        val items = page.items.map { i ->
            ListProviderResult.Item
                .newBuilder()
                .setActiveGames(i.activeGameCount)
                .setTotalGames(i.totalGameCount)
                .setProvider(
                    com.nekzabirov.igambling.proto.dto.ProviderDto.newBuilder()
                        .setId(i.id.toString())
                        .setIdentity(i.identity)
                        .setName(i.name)
                        .putAllImages(i.images)
                        .setOrder(i.order)
                        .setActive(i.active)
                        .also { builder -> i.aggregatorId?.let { builder.setAggregatorId(it.toString()) } }
                        .build()
                )
                .build()
        }

        // Get unique aggregators from results
        val aggregatorIds = page.items.mapNotNull { it.aggregatorId }.distinct()
        val aggregators = aggregatorIds.mapNotNull { aggId ->
            page.items.find { it.aggregatorId == aggId }?.let { item ->
                com.nekzabirov.igambling.proto.dto.AggregatorDto.newBuilder()
                    .setId(aggId.toString())
                    .setIdentity(item.aggregatorName ?: "")
                    .setType(item.aggregatorName ?: "")
                    .build()
            }
        }

        return ListProviderResult.newBuilder()
            .setTotalPage(page.totalPages.toInt())
            .addAllItems(items)
            .addAllAggregators(aggregators)
            .build()
    }

    override suspend fun update(request: UpdateProviderConfig): EmptyResult =
        updateProviderCommandHandler.handle(
            UpdateProviderCommand(identity = request.identity, order = request.order, active = request.active)
        )
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun updateImage(request: UpdateProviderImageCommand): EmptyResult =
        updateProviderImageCommandHandler.handle(
            application.port.inbound.command.UpdateProviderImageCommand(
                identity = request.identity,
                key = request.key,
                mediaFile = MediaFile(
                    ext = request.ext,
                    bytes = request.data.toByteArray()
                )
            )
        )
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }
}
