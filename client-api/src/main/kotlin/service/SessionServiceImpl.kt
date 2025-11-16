package service

import com.nekzabirov.igambling.proto.service.OpenSessionCommand
import com.nekzabirov.igambling.proto.service.OpenSessionResult
import com.nekzabirov.igambling.proto.service.SessionGrpcKt
import core.value.Currency
import core.value.Locale
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.*
import mapper.toPlatform
import org.koin.ktor.ext.get
import app.usecase.OpenSessionUsecase

class SessionServiceImpl(application: Application) : SessionGrpcKt.SessionCoroutineImplBase() {
    private val openSessionUsecase = application.get<OpenSessionUsecase>()

    override suspend fun openSession(request: OpenSessionCommand): OpenSessionResult =
        openSessionUsecase(
            gameIdentity = request.gameIdentity,
            playerId = request.playerId,
            currency = Currency(request.currency),
            locale = Locale(request.locale),
            platformN = request.platform.toPlatform(),
            lobbyUrl = request.lobbyUrl
        ).map { OpenSessionResult.newBuilder().setLaunchUrl(it.launchUrl).build() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }
}
