package infrastructure.api.grpc.mapper

import domain.collection.model.Collection
import com.nekzabirov.igambling.proto.dto.CollectionDto

fun Collection.toCollectionProto() = CollectionDto.newBuilder()
    .setId(this.id.toString())
    .setIdentity(this.identity)
    .setActive(this.active)
    .putAllName(this.name.data)
    .putAllImages(this.images.data)
    .build()