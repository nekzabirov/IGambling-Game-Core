package mapper

import com.nekzabirov.igambling.proto.dto.AggregatorDto
import domain.aggregator.model.AggregatorInfo

fun AggregatorInfo.toAggregatorProto() = AggregatorDto.newBuilder()
    .setId(this.id.toString())
    .setIdentity(this.identity)
    .putAllConfig(this.config)
    .setActive(this.active)
    .setType(this.aggregator.name)
    .build()