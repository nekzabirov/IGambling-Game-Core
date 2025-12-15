package com.nekgamebling.infrastructure.turbo.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BalanceTypeSerializer : KSerializer<BalanceType> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BalanceType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BalanceType) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): BalanceType {
        return BalanceType.entries.first { it.value == decoder.decodeString() }
    }
}

@Serializable(with = BalanceTypeSerializer::class)
enum class BalanceType(val value: String) {
    REAL("real"),
    BONUS("bonus"),
    LOCKED("locked")
}
