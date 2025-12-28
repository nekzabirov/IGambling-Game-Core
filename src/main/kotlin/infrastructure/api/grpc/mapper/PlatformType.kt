package infrastructure.api.grpc.mapper

import domain.common.value.Platform
import com.nekzabirov.igambling.proto.dto.PlatformType

fun Platform.toPlatformProto() = when(this) {
    Platform.DESKTOP -> PlatformType.PLATFORM_TYPE_DESKTOP
    Platform.MOBILE -> PlatformType.PLATFORM_TYPE_MOBILE
    Platform.DOWNLOAD -> PlatformType.PLATFORM_TYPE_DOWNLOAD
}

fun PlatformType.toPlatform() = when(this) {
    PlatformType.PLATFORM_TYPE_DESKTOP -> Platform.DESKTOP
    PlatformType.PLATFORM_TYPE_MOBILE -> Platform.MOBILE
    PlatformType.PLATFORM_TYPE_DOWNLOAD -> Platform.DOWNLOAD
    PlatformType.UNRECOGNIZED -> throw IllegalArgumentException("Unrecognized platform type")
}