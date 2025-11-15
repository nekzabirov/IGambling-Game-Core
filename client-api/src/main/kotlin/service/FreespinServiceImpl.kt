package service

import com.google.protobuf.Struct
import com.google.protobuf.Value
import com.nekzabirov.igambling.proto.service.FreespinGrpcKt
import com.nekzabirov.igambling.proto.service.GetPresetCommand
import com.nekzabirov.igambling.proto.service.GetPresetResult
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.*
import org.koin.ktor.ext.get
import usecase.GetPresetUsecase

class FreespinServiceImpl(application: Application) : FreespinGrpcKt.FreespinCoroutineImplBase() {
    private val getPresetUsecase = application.get<GetPresetUsecase>()

    override suspend fun getPreset(request: GetPresetCommand): GetPresetResult =
        getPresetUsecase(gameIdentity = request.gameIdentity)
            .map {
                GetPresetResult.newBuilder()
                    .setPreset(mapToStruct(it.preset))
                    .build()
            }
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
