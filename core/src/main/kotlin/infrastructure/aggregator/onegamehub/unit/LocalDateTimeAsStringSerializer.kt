package infrastructure.aggregator.onegamehub.unit

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object LocalDateTimeAsStringSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        val formatted = String.format(
            "%04d-%02d-%02d %02d:%02d:%02d",
            value.year,
            value.monthNumber,
            value.dayOfMonth,
            value.hour,
            value.minute,
            value.second
        )
        encoder.encodeString(formatted)
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val string = decoder.decodeString()
        val parts = string.split(" ")
        val dateParts = parts[0].split("-")
        val timeParts = parts[1].split(":")

        return LocalDateTime(
            year = dateParts[0].toInt(),
            monthNumber = dateParts[1].toInt(),
            dayOfMonth = dateParts[2].toInt(),
            hour = timeParts[0].toInt(),
            minute = timeParts[1].toInt(),
            second = timeParts[2].toInt()
        )
    }
}