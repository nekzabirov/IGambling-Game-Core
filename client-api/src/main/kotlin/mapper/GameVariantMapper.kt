package mapper

import com.nekzabirov.igambling.proto.dto.GameVariantDto
import domain.game.model.GameVariant

fun GameVariant.toGameVariantProto(): GameVariantDto = GameVariantDto.newBuilder()
    .setId(this.id.toString())
    .setGameId(this.gameId.toString())
    .setSymbol(this.symbol)
    .setName(this.name)
    .setProviderName(this.providerName)
    .setFreeChipEnable(this.freeChipEnable)
    .setFreeChipEnable(this.freeSpinEnable)
    .setJackpotEnable(this.jackpotEnable)
    .setDemoEnable(this.demoEnable)
    .setBonusBuyEnable(this.bonusBuyEnable)
    .addAllLocales(this.locales)
    .addAllPlatforms(this.platforms.map { it.toPlatformProto() })
    .setAggregator(this.aggregator.name)
    .build()