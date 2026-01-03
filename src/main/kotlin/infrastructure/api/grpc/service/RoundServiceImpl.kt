package infrastructure.api.grpc.service

import application.port.inbound.query.*
import com.google.protobuf.Timestamp
import com.nekzabirov.igambling.proto.service.GameWithDetailsDto
import com.nekzabirov.igambling.proto.service.GetRoundsDetailsCommand
import com.nekzabirov.igambling.proto.service.GetRoundsDetailsResult
import com.nekzabirov.igambling.proto.service.RoundGrpcKt
import domain.session.repository.RoundFilter
import infrastructure.handler.query.*
import io.ktor.server.application.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.koin.ktor.ext.get
import shared.value.Pageable
import com.nekzabirov.igambling.proto.service.RoundDetailsDto

class RoundServiceImpl(application: Application) : RoundGrpcKt.RoundCoroutineImplBase() {
    private val getRoundsDetailsQueryHandler = application.get<GetRoundsDetailsQueryHandler>()

    override suspend fun getRoundsDetails(request: GetRoundsDetailsCommand): GetRoundsDetailsResult {
        val filter = RoundFilter.builder().apply {
            if (request.hasPlayerId()) {
                playerId(request.playerId)
            }
            if (request.hasGameIdentity()) {
                gameIdentity(request.gameIdentity)
            }
        }.build()

        val page = getRoundsDetailsQueryHandler.handle(
            GetRoundsDetailsQuery(
                pageable = Pageable(request.pageNumber, request.pageSize),
                filter = filter
            )
        )

        return GetRoundsDetailsResult.newBuilder()
            .addAllItems(page.items.map { it.toRoundDetailsProto() })
            .setTotalPage(page.totalPages.toInt())
            .setTotalItems(page.totalItems)
            .setCurrentPage(page.currentPage)
            .build()
    }

    private fun RoundDetailsReadModel.toRoundDetailsProto(): RoundDetailsDto {
        val gameDto = GameWithDetailsDto.newBuilder()
            .setIdentity(this.gameIdentity)
            .setName(this.gameName)
            .build()

        val builder = RoundDetailsDto.newBuilder()
            .setId(this.id.toString())
            .setPlaceAmount(this.placeAmount.toString())
            .setSettleAmount(this.settleAmount.toString())
            .setCurrency(this.currency)
            .setGame(gameDto)
            .setIsFinished(this.isFinished)
            .setCreatedAt(this.createdAt.toTimestamp())

        this.freeSpinId?.let { builder.setFreeSpinId(it) }
        this.finishedAt?.let { builder.setFinishedAt(it.toTimestamp()) }

        return builder.build()
    }

    private fun LocalDateTime.toTimestamp(): Timestamp {
        val instant = this.toInstant(TimeZone.UTC)
        return Timestamp.newBuilder()
            .setSeconds(instant.epochSeconds)
            .setNanos(instant.nanosecondsOfSecond)
            .build()
    }
}
