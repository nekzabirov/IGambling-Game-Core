package infrastructure.api.grpc.service

import application.service.OpenSessionCommand
import application.service.SessionService
import shared.value.Currency
import domain.common.value.Locale
import com.nekzabirov.igambling.proto.service.OpenSessionCommand as ProtoOpenSessionCommand
import com.nekzabirov.igambling.proto.service.OpenSessionResult
import com.nekzabirov.igambling.proto.service.SessionGrpcKt
import io.grpc.Status
import io.grpc.StatusException
import io.ktor.server.application.*
import infrastructure.api.grpc.mapper.toPlatform
import org.koin.ktor.ext.get

class SessionServiceImpl(application: Application) : SessionGrpcKt.SessionCoroutineImplBase() {
    private val sessionService = application.get<SessionService>()

    override suspend fun openSession(request: ProtoOpenSessionCommand): OpenSessionResult =
        sessionService.open(
            OpenSessionCommand(
                gameIdentity = request.gameIdentity,
                playerId = request.playerId,
                currency = Currency(request.currency),
                locale = Locale(request.locale),
                platform = request.platform.toPlatform(),
                lobbyUrl = request.lobbyUrl
            )
        ).map { OpenSessionResult.newBuilder().setLaunchUrl(it.launchUrl).build() }
            .getOrElse { throw StatusException(Status.INVALID_ARGUMENT.withDescription(it.message)) }
}
