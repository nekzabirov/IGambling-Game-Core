package service

import com.google.protobuf.Struct
import com.google.protobuf.Value
import com.nekzabirov.igambling.proto.dto.EmptyResult
import com.nekzabirov.igambling.proto.service.CancelFreespinCommand
import com.nekzabirov.igambling.proto.service.CreateFreespinCommand
import com.nekzabirov.igambling.proto.service.FreespinGrpcKt
import com.nekzabirov.igambling.proto.service.GetPresetCommand
import com.nekzabirov.igambling.proto.service.GetPresetResult
import core.value.Currency
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.*
import org.koin.ktor.ext.get
import app.usecase.CancelFreespinUsecase
import app.usecase.CreateFreespinUsecase
import app.usecase.GetPresetUsecase
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FreespinServiceImpl(application: Application) : FreespinGrpcKt.FreespinCoroutineImplBase() {
    private val getPresetUsecase = application.get<GetPresetUsecase>()
    private val createFreespinUsecase = application.get<CreateFreespinUsecase>()
    private val cancelFreespinUsecase = application.get<CancelFreespinUsecase>()

    override suspend fun getPreset(request: GetPresetCommand): GetPresetResult =
        getPresetUsecase(gameIdentity = request.gameIdentity)
            .map {
                GetPresetResult.newBuilder()
                    .setPreset(mapToStruct(it.preset))
                    .build()
            }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun createFreespin(request: CreateFreespinCommand): EmptyResult =
        createFreespinUsecase(
            presetValue = request.presetValueMap,
            referenceId = request.referenceId,
            playerId = request.playerId,
            gameIdentity = request.gameIdentity,
            currency = Currency(request.currency),
            startAt = Instant.fromEpochSeconds(request.startAt.seconds, request.startAt.nanos)
                .toLocalDateTime(TimeZone.UTC),
            endAt = Instant.fromEpochSeconds(request.endAt.seconds, request.endAt.nanos)
                .toLocalDateTime(TimeZone.UTC)
        )
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    override suspend fun cancelFreespin(request: CancelFreespinCommand): EmptyResult =
        cancelFreespinUsecase(
            referenceId = request.referenceId,
            gameIdentity = request.gameIdentity
        )
            .map { EmptyResult.getDefaultInstance() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }

    private fun mapToStruct(map: Map<String, Any?>): Struct {
        val structBuilder = Struct.newBuilder()
        map.forEach { (key, value) ->
            structBuilder.putFields(key, anyToValue(value))
        }
        return structBuilder.build()
    }

    private fun anyToValue(value: Any?): Value {
        return when (value) {
            null -> Value.newBuilder().setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build()
            is String -> Value.newBuilder().setStringValue(value).build()
            is Int -> Value.newBuilder().setNumberValue(value.toDouble()).build()
            is Long -> Value.newBuilder().setNumberValue(value.toDouble()).build()
            is Double -> Value.newBuilder().setNumberValue(value).build()
            is Float -> Value.newBuilder().setNumberValue(value.toDouble()).build()
            is Boolean -> Value.newBuilder().setBoolValue(value).build()
            is Map<*, *> -> Value.newBuilder().setStructValue(mapToStruct(value as Map<String, Any?>)).build()
            is List<*> -> {
                val listBuilder = com.google.protobuf.ListValue.newBuilder()
                value.forEach { listBuilder.addValues(anyToValue(it)) }
                Value.newBuilder().setListValue(listBuilder.build()).build()
            }
            else -> Value.newBuilder().setStringValue(value.toString()).build()
        }
    }
}
