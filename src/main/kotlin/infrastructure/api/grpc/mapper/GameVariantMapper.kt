package infrastructure.api.grpc.mapper

import domain.game.model.GameVariant
import com.nekzabirov.igambling.proto.dto.GameVariantDto

fun GameVariant.toGameVariantProto(): GameVariantDto = GameVariantDto.newBuilder()
    .setId(this.id.toString())
    .setGameId(this.gameId?.toString() ?: "")
    .setSymbol(this.symbol)
    .setName(this.name)
    .setProviderName(this.providerName)
    .setFreeChipEnable(this.freeChipEnable)
    .setFreeSpinEnable(this.freeSpinEnable)
    .setJackpotEnable(this.jackpotEnable)
    .setDemoEnable(this.demoEnable)
    .setBonusBuyEnable(this.bonusBuyEnable)
    .addAllLocales(this.locales.map { it.value })
    .addAllPlatforms(this.platforms.map { it.toPlatformProto() })
    .setAggregator(this.aggregator.name)
    .build()