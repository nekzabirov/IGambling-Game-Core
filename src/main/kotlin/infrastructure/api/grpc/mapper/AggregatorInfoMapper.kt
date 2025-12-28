package infrastructure.api.grpc.mapper

import domain.aggregator.model.AggregatorInfo
import com.nekzabirov.igambling.proto.dto.AggregatorDto

fun AggregatorInfo.toAggregatorProto() = AggregatorDto.newBuilder()
    .setId(this.id.toString())
    .setIdentity(this.identity)
    .putAllConfig(this.config)
    .setActive(this.active)
    .setType(this.aggregator.name)
    .build()