package com.nekgamebling.infrastructure.api.grpc.service

import com.nekgamebling.application.usecase.session.OpenSessionCommand
import com.nekgamebling.application.usecase.session.OpenSessionUsecase
import com.nekgamebling.shared.value.Currency
import com.nekgamebling.shared.value.Locale
import com.nekzabirov.igambling.proto.service.OpenSessionCommand as ProtoOpenSessionCommand
import com.nekzabirov.igambling.proto.service.OpenSessionResult
import com.nekzabirov.igambling.proto.service.SessionGrpcKt
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.*
import com.nekgamebling.infrastructure.api.grpc.mapper.toPlatform
import org.koin.ktor.ext.get

class SessionServiceImpl(application: Application) : SessionGrpcKt.SessionCoroutineImplBase() {
    private val openSessionUsecase = application.get<OpenSessionUsecase>()

    override suspend fun openSession(request: ProtoOpenSessionCommand): OpenSessionResult =
        openSessionUsecase(
            OpenSessionCommand(
                gameIdentity = request.gameIdentity,
                playerId = request.playerId,
                currency = Currency(request.currency),
                locale = Locale(request.locale),
                platform = request.platform.toPlatform(),
                loggyUrl = request.lobbyUrl
            )
        ).map { OpenSessionResult.newBuilder().setLaunchUrl(it.launchUrl).build() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }
}
