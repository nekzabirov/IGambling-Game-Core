package infrastructure.api.grpc.service

import application.usecase.spin.GetRoundsDetailsUsecase
import com.nekzabirov.igambling.proto.service.GetRoundsDetailsCommand
import com.nekzabirov.igambling.proto.service.GetRoundsDetailsResult
import com.nekzabirov.igambling.proto.service.RoundGrpcKt
import domain.session.model.RoundDetails
import domain.session.repository.RoundFilter
import infrastructure.api.grpc.mapper.toAggregatorProto
import infrastructure.api.grpc.mapper.toProviderProto
import io.ktor.server.application.*
import org.koin.ktor.ext.get
import shared.value.Pageable
import com.nekzabirov.igambling.proto.service.GameWithDetailsDto
import com.nekzabirov.igambling.proto.service.RoundDetailsDto

class RoundServiceImpl(application: Application) : RoundGrpcKt.RoundCoroutineImplBase() {
    private val getRoundsDetailsUsecase = application.get<GetRoundsDetailsUsecase>()

    override suspend fun getRoundsDetails(request: GetRoundsDetailsCommand): GetRoundsDetailsResult {
        val filter = RoundFilter.builder().apply {
            if (request.hasPlayerId()) {
                playerId(request.playerId)
            }
            if (request.hasGameIdentity()) {
                gameIdentity(request.gameIdentity)
            }
        }.build()

        val page = getRoundsDetailsUsecase(
            pageable = Pageable(request.pageNumber, request.pageSize),
            filter = filter
        )

        return GetRoundsDetailsResult.newBuilder()
            .addAllItems(page.items.map { it.toRoundDetailsProto() })
            .setTotalPage(page.totalPages.toInt())
            .setTotalItems(page.totalItems)
            .setCurrentPage(page.currentPage)
            .build()
    }

    private fun RoundDetails.toRoundDetailsProto(): RoundDetailsDto {
        val builder = RoundDetailsDto.newBuilder()
            .setId(this.id.toString())
            .setPlaceAmount(this.placeAmount.toString())
            .setSettleAmount(this.settleAmount.toString())
            .setCurrency(this.currency.value)
            .setGame(this.game.toGameWithDetailsProto())
            .setIsFinished(this.isFinished)

        this.freeSpinId?.let { builder.setFreeSpinId(it) }

        return builder.build()
    }

    private fun domain.game.model.GameWithDetails.toGameWithDetailsProto(): GameWithDetailsDto =
        GameWithDetailsDto.newBuilder()
            .setId(this.id.toString())
            .setIdentity(this.identity)
            .setName(this.name)
            .putAllImages(this.images.data)
            .setBonusBetEnable(this.bonusBetEnable)
            .setBonusWageringEnable(this.bonusWageringEnable)
            .addAllTags(this.tags)
            .setSymbol(this.symbol)
            .setFreeSpinEnable(this.freeSpinEnable)
            .setFreeChipEnable(this.freeChipEnable)
            .setJackpotEnable(this.jackpotEnable)
            .setDemoEnable(this.demoEnable)
            .setBonusBuyEnable(this.bonusBuyEnable)
            .addAllLocales(this.locales.map { it.value })
            .addAllPlatforms(this.platforms.map { it.name })
            .setPlayLines(this.playLines)
            .setProvider(this.provider.toProviderProto())
            .setAggregator(this.aggregator.toAggregatorProto())
            .build()
}
