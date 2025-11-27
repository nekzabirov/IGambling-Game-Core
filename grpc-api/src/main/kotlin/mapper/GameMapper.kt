package mapper

import com.nekzabirov.igambling.proto.dto.GameDto
import domain.game.model.Game

fun Game.toGameProto() = GameDto.newBuilder()
    .setId(this.id.toString())
    .setIdentity(this.identity)
    .setName(this.name)
    .setProviderId(this.providerId.toString())
    .setBonusBetEnable(this.bonusBetEnable)
    .setBonusWageringEnable(this.bonusWageringEnable)
    .setActive(this.active)
    .addAllTags(this.tags)
    .putAllImages(this.images.data)
    .build()